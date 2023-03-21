package com.yuyin.demo.view.edit

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.yuyin.demo.R
import com.yuyin.demo.databinding.FragmentEditBinding
import com.yuyin.demo.models.AudioPlay
import com.yuyin.demo.models.LocalResult
import com.yuyin.demo.models.ResultItem
import com.yuyin.demo.viewmodel.EditViewModel
import com.yuyin.demo.viewmodel.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.ArrayBlockingQueue
import kotlin.io.path.absolutePathString
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log


class EditFragment : Fragment() {
    private var filePath: String = ""
    val TAG = "EditFragment"
    private var _binding: FragmentEditBinding? = null
    val binding get() = _binding!!
    val yuyinViewModel: YuyinViewModel by activityViewModels()
    val model: EditViewModel by viewModels()
    lateinit var adapter: ResultAdapter
    var currentAudioItem = ArrayBlockingQueue<Int>(1)
    lateinit var startIcon: Drawable
    lateinit var endIcon: Drawable
    lateinit var playLabel: String
    lateinit var stopLabel: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        arguments?.let {
            filePath = it.getString("file").toString()
        }
        Log.i(TAG, "onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        Log.i(TAG, "onCreateView")
        initMenu()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated")
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "not exit open file, come back")
            findNavController().popBackStack()
        } else {
            with(file) {
                val json = readText(StandardCharsets.UTF_8)
                val jsonAdapter = yuyinViewModel.moshi.adapter(LocalResult::class.java)
                model.localResult = jsonAdapter.fromJson(json) ?: LocalResult.emptyLocalResult
                if (model.localResult == LocalResult.emptyLocalResult) {
                    Log.e(TAG, "emptyLocalResult, come back")
                    findNavController().popBackStack()
                }
                model.audioResource = File(yuyinViewModel.yuYinDataDir.absolutePathString(),model.localResult.audioFile)
            }
        }
        with(model) {
            val linearLayoutManager = LinearLayoutManager(context)
            binding.recyclerShowTextAndAudio.layoutManager = linearLayoutManager
            val results = mutableListOf<ResultItem>()
            startIcon = AppCompatResources.getDrawable(requireContext(), R.drawable.play_icon36)!!
            endIcon = AppCompatResources.getDrawable(requireContext(), R.drawable.stop_icon36)!!
            playLabel = getString(R.string.play)
            stopLabel = getString(R.string.stop)
            model.viewModelScope.launch(Dispatchers.Default) {
                for (i in localResult.speechText.indices) {
                    localResult.run {
                        results.add(ResultItem(speechText[i], start[i], end[i]))
                    }
                }
                adapter = ResultAdapter(
                    results,
                    model,
                    startIcon,
                    endIcon,
                    lifecycleScope,
                    playLabel,
                    stopLabel
                )
                withContext(Dispatchers.Main) {
                    binding.recyclerShowTextAndAudio.adapter = adapter
                }
            }
        }
        observeAudioItem()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView")
        model.viewModelScope.launch {
            if (currentAudioItem.isNotEmpty()) {
                model.audioConfig.emit(AudioPlay.AudioConfig(0,0,AudioPlay.AudioConfigState.STOP, currentAudioItem.take()))
            }
        }
        _binding = null
    }

    private fun initMenu() {
        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.run_asr_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.save_option -> {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            var file =  File(filePath)
                            if (binding.titleText.text.isNullOrBlank()) {
                                binding.titleText.text.toString().run {
                                    if (this != file.nameWithoutExtension) {
                                        file.deleteOnExit()
                                        file = File(yuyinViewModel.yuYinDataDir.absolutePathString(),this)
                                    }
                                }
                            }
                            val jsonAdapter = yuyinViewModel.moshi.adapter(LocalResult::class.java)
                            val json = jsonAdapter.toJson(model.localResult)
                            with(file) {
                                writeText(json, StandardCharsets.UTF_8)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }


    private fun observeAudioItem() {
        viewLifecycleOwner.lifecycleScope.launch {
            model.audioConfig.collect { audioConfig ->
                when (audioConfig.State) {
                    AudioPlay.AudioConfigState.PLAY -> {
                        if (currentAudioItem.isEmpty()) {
                            // item1 want change to stop, others is in init
                            currentAudioItem.add(audioConfig.id)
                            Log.i(TAG, "${audioConfig.id} play->stop; play")
                        } else {
                            if (audioConfig.id != currentAudioItem.peek()) {
                                // item1 is stop but item2 want change to stop
                                // init item1 and then change item2
                                val preId = currentAudioItem.take()
                                currentAudioItem.add(audioConfig.id)
                                Log.i(TAG, "$preId play->stop; ${audioConfig.id} stop->play")
                            } else {
                                Log.e(
                                    TAG,
                                    "error ${audioConfig.id} and ${currentAudioItem.peek()} is same"
                                )
                            }
                        }
                    }
                    AudioPlay.AudioConfigState.STOP -> {
                        if (currentAudioItem.isEmpty()) {
                            // 不可能为空
                            Log.e(
                                TAG,
                                "error currentAudioItem is empty"
                            )
                        } else {
                            if (audioConfig.id == currentAudioItem.peek()) {
                                // 只有一个item处于stop
                                Log.i(TAG, "${currentAudioItem.take()} stop->play, play")
                            } else {
                                // 不可能不相等
                                Log.e(
                                    TAG,
                                    "error currentAudioItem is not equal to audioConfig id"
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}