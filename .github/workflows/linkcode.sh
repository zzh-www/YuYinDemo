rm app/src/main/cpp/bin && mv --force wenet/runtime/core/bin app/src/main/cpp/bin
rm app/src/main/cpp/cmake && mv --force wenet/runtime/core/cmake app/src/main/cpp/cmake
rm app/src/main/cpp/decoder && mv --force wenet/runtime/core/decoder app/src/main/cpp/decoder
rm app/src/main/cpp/frontend && mv --force wenet/runtime/core/frontend app/src/main/cpp/frontend
rm app/src/main/cpp/kaldi && mv --force wenet/runtime/core/kaldi app/src/main/cpp/kaldi
rm app/src/main/cpp/patch && mv --force wenet/runtime/core/patch app/src/main/cpp/patch
rm app/src/main/cpp/post_processor && mv --force wenet/runtime/core/post_processor app/src/main/cpp/post_processor
rm app/src/main/cpp/utils && mv --force wenet/runtime/core/utils app/src/main/cpp/utils
ls -al app/src/main/cpp/
