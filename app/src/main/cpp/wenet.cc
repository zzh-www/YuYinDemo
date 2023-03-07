// Copyright (c) 2021 Mobvoi Inc (authors: Xiaoyu Chen)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
#include <jni.h>

#include "torch/script.h"
#include "torch/torch.h"

#include "decoder/asr_decoder.h"
#include "decoder/torch_asr_model.h"
#include "frontend/feature_pipeline.h"
#include "frontend/wav.h"
#include "post_processor/post_processor.h"
#include "utils/log.h"
#include <cstring>
#include <future>
#include "queue"

namespace wenet {

    std::shared_ptr<DecodeOptions> decode_config;
    std::shared_ptr<FeaturePipelineConfig> feature_config;
    std::shared_ptr<FeaturePipeline> feature_pipeline;
    std::shared_ptr<AsrDecoder> decoder;
    std::shared_ptr<DecodeResource> resource;
    DecodeState state = kEndFeats;
    std::string total_result;  // NOLINT
    JavaVM *javaVM = nullptr;

    typedef struct {
        jclass native_asr_listener_call_clazz;
        jmethodID on_speech_result_receive;
        jmethodID on_speech_part_result_receive;
        jmethodID on_model_init;
        jmethodID on_model_reset;
        jmethodID on_model_finish;

        jclass asr_speech_result;
        jmethodID asr_speech_result_init;
    } asr_callback;

// 定义承载 listener 和 speech_result 的 类和方法实例
    static asr_callback *asr_Callback;

    jbyteArray stringToJByteArray(JNIEnv *env, std::string s) {
        jbyteArray arrayByte = (*env).NewByteArray(s.length());
        auto *singleByte = (jbyte *) s.c_str();
        env->SetByteArrayRegion(arrayByte, 0, s.length(), singleByte);
        return arrayByte;
    }

    void callBackViewModel(JNIEnv *env) {

        // 获得回调接口 函数 onSpeechResultReceive 的methodID
        jclass clazz_listener = env->FindClass("com/mobvoi/wenet/Recognize");
        jmethodID method_speech_result_receive = env->GetStaticMethodID(
                clazz_listener,
                "resultReceiveListener",
                "(Lcom/yuyin/demo/models/SpeechResult;)V");
        // 获取自定义 SpeechResult 的构造函数
        jclass clazz_speech_result = env->FindClass("com/yuyin/demo/models/SpeechResult");
        jmethodID method_speech_result_init = env->GetMethodID(clazz_speech_result, "<init>",
                                                               "([BIII)V");
        jmethodID method_on_model_init = env->GetStaticMethodID(
                clazz_listener,
                "modelInitListener",
                "(Z)V");

        jmethodID method_on_model_reset = env->GetStaticMethodID(
                clazz_listener,
                "modelResetListener",
                "(Z)V");

        jmethodID method_on_model_finish = env->GetStaticMethodID(
                clazz_listener,
                "modelFinishListener",
                "(Z)V");
        jmethodID method_on_speech_part = env->GetStaticMethodID(
                clazz_listener,
                "partResultReceiveListener",
                "([B)V");
        // 创建callback 实例
        asr_Callback = (asr_callback *) malloc(sizeof(asr_callback));
        memset(asr_Callback, 0, sizeof(asr_callback));
        asr_Callback->native_asr_listener_call_clazz = (jclass) env->NewGlobalRef(clazz_listener);
        asr_Callback->on_speech_result_receive = method_speech_result_receive;
        asr_Callback->on_model_init = method_on_model_init;
        asr_Callback->on_model_reset = method_on_model_reset;
        asr_Callback->on_model_finish = method_on_model_finish;
        asr_Callback->on_speech_part_result_receive = method_on_speech_part;
        asr_Callback->asr_speech_result = (jclass) env->NewGlobalRef(clazz_speech_result);
        asr_Callback->asr_speech_result_init = method_speech_result_init;

        // 销毁局部引用
        env->DeleteLocalRef(clazz_listener);
        env->DeleteLocalRef(clazz_speech_result);
    }

    void init(JNIEnv *env, jobject thiz, jstring jModelPath, jstring jDictPath) {
        callBackViewModel(env);
        env->CallStaticVoidMethod(
                asr_Callback->native_asr_listener_call_clazz,
                asr_Callback->on_model_init,
                JNI_FALSE);
        resource = std::make_shared<DecodeResource>();
        resource->model = std::make_shared<TorchAsrModel>();
        const char *pModelPath = (env)->GetStringUTFChars(jModelPath, nullptr);
        std::string modelPath = std::string(pModelPath);
        const char *pDictPath = (env)->GetStringUTFChars(jDictPath, nullptr);
        std::string dictPath = std::string(pDictPath);

        resource = std::make_shared<DecodeResource>();
        resource->model = std::make_shared<TorchAsrModel>();

        // init model
        auto model = std::make_shared<TorchAsrModel>();
        model->Read(modelPath);

        // init dict
        resource = std::make_shared<DecodeResource>();
        resource->model = model;
        resource->symbol_table = std::shared_ptr<fst::SymbolTable>(
                fst::SymbolTable::ReadText(dictPath));
        resource->unit_table = std::shared_ptr<fst::SymbolTable>(
                fst::SymbolTable::ReadText(dictPath));

        PostProcessOptions post_process_opts;
        resource->post_processor =
                std::make_shared<PostProcessor>(post_process_opts);

        feature_config = std::make_shared<FeaturePipelineConfig>(80, 16000);
        feature_pipeline = std::make_shared<FeaturePipeline>(*feature_config);

        decode_config = std::make_shared<DecodeOptions>();
        decode_config->chunk_size = 16;

        decoder = std::make_shared<AsrDecoder>(feature_pipeline, resource,
                                               *decode_config);
        env->CallStaticVoidMethod(
                asr_Callback->native_asr_listener_call_clazz,
                asr_Callback->on_model_init,
                JNI_TRUE);
    }

    void reset(JNIEnv *env, jobject clazz) {
        env->CallStaticVoidMethod(
                asr_Callback->native_asr_listener_call_clazz,
                asr_Callback->on_model_reset,
                JNI_FALSE);
        LOG(INFO) << "wenet reset";
        decoder->Reset();
        total_result = "";
        env->CallStaticVoidMethod(
                asr_Callback->native_asr_listener_call_clazz,
                asr_Callback->on_model_reset,
                JNI_TRUE);
    }

    void accept_waveform(JNIEnv *env, jobject clazz, jshortArray jWaveform) {
        jsize size = env->GetArrayLength(jWaveform);
        int16_t *waveform = env->GetShortArrayElements(jWaveform, 0);
        feature_pipeline->AcceptWaveform(waveform, size);
    }

    void set_input_finished(JNIEnv *env, jobject thiz) {
        feature_pipeline->set_input_finished();
    }

    void decode_thread_func(std::promise<int> &promiseObj) {
        JNIEnv *env = nullptr;
        if (javaVM == nullptr) {
            promiseObj.set_value(JNI_ERR);
            return;
        }
        int envGetResult = javaVM->GetEnv((void **) &env, JNI_VERSION_1_6);
        if (envGetResult == JNI_EDETACHED || env == nullptr) {
            envGetResult = javaVM->AttachCurrentThread(&env, nullptr);
            if (envGetResult < 0) {
                env = nullptr;
                promiseObj.set_value(JNI_ERR);
                return;
            } else {
                promiseObj.set_value(JNI_OK);
            }
        }
        env->CallStaticVoidMethod(
                asr_Callback->native_asr_listener_call_clazz,
                asr_Callback->on_model_finish,
                JNI_FALSE);
        LOG(INFO)<<"finish false";
        while (true) {
            state = decoder->Decode();
            if (state == kEndFeats || state == kEndpoint) {
                decoder->Rescoring();
            }

            std::string result;
            std::vector<WordPiece> words;
            int level;
            if (decoder->DecodedSomething()) {
                result = decoder->result()[0].sentence;
            }

            if (state == kEndFeats) {
                words = decoder->result()[0].word_pieces;
                level = 3;
                LOG(INFO) << "wenet end feats final result: " << result;
                int start;
                int end;
                if (!words.empty()) {
                    level += 1;
                    start = words[0].start;
                    end = words[words.size() - 1].end;
                    jobject speechResult = env->NewObject(asr_Callback->asr_speech_result,
                                                          asr_Callback->asr_speech_result_init,
                                                          stringToJByteArray(env, result), start, end, level);
                    env->CallStaticVoidMethod(asr_Callback->native_asr_listener_call_clazz,
                                              asr_Callback->on_speech_result_receive, speechResult);
                    total_result += result;
                }
                LOG(INFO) << "wenet end feats final result: " << total_result;
                break;
            } else if (state == kEndpoint) {
                words = decoder->result()[0].word_pieces;
                level = 1;
                LOG(INFO) << "wenet endpoint final result: " << result;
                int start = 0;
                int end = 0;
                total_result += result + ",";
                if (!words.empty()) {
                    level += 1;
                    start = words[0].start;
                    end = words[words.size() - 1].end;
                }

                jobject speechResult = env->NewObject(asr_Callback->asr_speech_result,
                                                      asr_Callback->asr_speech_result_init,
                                                      stringToJByteArray(env, result), start, end, level);
                env->CallStaticVoidMethod(asr_Callback->native_asr_listener_call_clazz,
                                          asr_Callback->on_speech_result_receive, speechResult);
                env->CallStaticVoidMethod(asr_Callback->native_asr_listener_call_clazz,
                                          asr_Callback->on_speech_part_result_receive, stringToJByteArray(env, "")); // hotText 为空
                decoder->ResetContinuousDecoding();
            } else {
                if (decoder->DecodedSomething()) {
                    env->CallStaticVoidMethod(asr_Callback->native_asr_listener_call_clazz,
                                              asr_Callback->on_speech_part_result_receive, stringToJByteArray(env, result));
                }
            }
        }
        LOG(INFO)<<"finish true";
        env->CallStaticVoidMethod(
                asr_Callback->native_asr_listener_call_clazz,
                asr_Callback->on_model_finish,
                JNI_TRUE);
        javaVM->DetachCurrentThread();
    }

    void start_decode(JNIEnv *env, jobject thiz) {
        std::promise<int> promiseObj;
        std::future<int> futureObj = promiseObj.get_future();
        std::thread decode_thread(decode_thread_func, std::ref(promiseObj));
        decode_thread.detach();
        if (futureObj.get() == JNI_ERR) {
            LOG(ERROR) << "error get env";
        }
    }

    jboolean get_finished(JNIEnv *env, jobject clazz) {
        if (state == kEndFeats) {
            LOG(INFO) << "wenet recognize finished";
            return JNI_TRUE;
        }
        return JNI_FALSE;
    }


// 更改以获取段句
    jbyteArray get_result(JNIEnv *env, jobject clazz) {
        jbyteArray arrayByte = (*env).NewByteArray(total_result.length());
        auto *singleByte = (jbyte *) total_result.c_str();
        env->SetByteArrayRegion(arrayByte, 0, total_result.length(), singleByte);
        return arrayByte;
    }

// for test
    jbyteArray javaStringToJniArray(JNIEnv *env, jobject thiz, jstring str) {
        std::string s = std::string(env->GetStringUTFChars(str, JNI_FALSE));
        jbyteArray arrayByte = (*env).NewByteArray(s.length());
        auto *singleByte = (jbyte *) s.c_str();
        env->SetByteArrayRegion(arrayByte, 0, s.length(), singleByte);
        return arrayByte;
    }

}  // namespace wenet

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env;
    wenet::javaVM = vm;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass c = env->FindClass("com/mobvoi/wenet/Recognize");
    if (c == nullptr) {
        return JNI_ERR;
    }

    static const JNINativeMethod methods[] = {
            {"init",                 "(Ljava/lang/String;Ljava/lang/String;)V",
                                                               reinterpret_cast<void *>(wenet::init)},
            {"reset",                "()V",
                                                               reinterpret_cast<void *>(wenet::reset)},
            {"acceptWaveform",       "([S)V",
                                                               reinterpret_cast<void *>(wenet::accept_waveform)},
            {"setInputFinished",     "()V",
                                                               reinterpret_cast<void *>(wenet::set_input_finished)},
            {"getFinished",          "()Z",
                                                               reinterpret_cast<void *>(wenet::get_finished)},
            {"startDecode",          "()V",
                                                               reinterpret_cast<void *>(wenet::start_decode)},
            {"getByteResult",        "()[B",
                                                               reinterpret_cast<void *>(wenet::get_result)},
            {"javaStringToJniArray", "(Ljava/lang/String;)[B", reinterpret_cast<void *>(wenet::javaStringToJniArray)},
    };
    int rc = env->RegisterNatives(c, methods,
                                  sizeof(methods) / sizeof(JNINativeMethod));

    if (rc != JNI_OK) {
        return rc;
    }

    return JNI_VERSION_1_6;
}