package com.yuyin.demo.view

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.moshi.JsonAdapter
import com.yuyin.demo.R
import com.yuyin.demo.databinding.FragmentSettingsBinding
import com.yuyin.demo.models.LocalSettings
import com.yuyin.demo.viewmodel.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log



class SettingsFragment : Fragment() {
    val TAG = "SettingsFragment"
    private var _binding: FragmentSettingsBinding? = null
    val binding get() = _binding!!
    val yuyinViewModel: YuyinViewModel by activityViewModels()
    lateinit var modelKeys: MutableList<String>
    private var newDictUri: Uri? = null
    private var newModelUri: Uri? = null
    private var openDocumentPickerForDict =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                result.data.let { resultData ->

                    /**
                     * Upon getting a document uri returned, we can use
                     * [ContentResolver.takePersistableUriPermission] in order to persist the
                     * permission across restarts.
                     *
                     * This may not be necessary for your app. If the permission is not
                     * persisted, access to the uri is granted until the receiving Activity is
                     * finished. You can extend the lifetime of the permission grant by passing
                     * it along to another Android component. This is done by including the uri
                     * in the data field or the ClipData object of the Intent used to launch that
                     * component. Additionally, you need to add FLAG_GRANT_READ_URI_PERMISSION
                     * and/or FLAG_GRANT_WRITE_URI_PERMISSION to the Intent.
                     *
                     * This app takes the persistable URI permission grant to demonstrate how, and
                     * to allow us to reopen the last opened document when the app starts.
                     */
                    resultData?.data?.let { documentUri ->
                        requireActivity().contentResolver.takePersistableUriPermission(
                            documentUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile: DocumentFile =
                            DocumentFile.fromSingleUri(requireContext(), documentUri) ?: return@registerForActivityResult
                        val documentName = documentFile.name
                        val file = Paths.get(
                            yuyinViewModel.yuYinDirPath.absolutePathString(),
                            documentName
                        ).toFile()
                        if (file.exists()) {
                            Toast.makeText(requireContext(), "file exist", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            newDictUri = documentUri
                            binding.dictPathText.text = documentName
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "pick file error", Toast.LENGTH_LONG).show()
            }
        }
    private var openDocumentPickerForModel =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                result.data.let { resultData ->
                    resultData?.data?.let { documentUri ->
                        requireActivity().contentResolver.takePersistableUriPermission(
                            documentUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile: DocumentFile =
                            DocumentFile.fromSingleUri(requireContext(), documentUri) ?: return@registerForActivityResult
                        val documentName = documentFile.name
                        val file = Paths.get(
                            yuyinViewModel.yuYinDirPath.absolutePathString(),
                            documentName
                        ).toFile()
                        if (file.exists()) {
                            Toast.makeText(requireContext(), "file exist", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            newModelUri = documentUri
                            binding.modePathText.text = documentName
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "pick file error", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        initMenu()
        yuyinViewModel.newSettings = yuyinViewModel.settings.copy()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (yuyinViewModel.newSettings.modelMode == "自定义") {
            binding.chooseDict.isEnabled = true
            binding.chooseModel.isEnabled = true
        } else {
            binding.chooseDict.isEnabled = false
            binding.chooseModel.isEnabled = false
        }
        checkSwitch(false)
        initSwitch()
        initSpinner()
        initPickFileButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initSwitch() {
        when (yuyinViewModel.settings.saveMode) {
            0 -> binding.saveTextSwitch.let {
                it.isChecked = true
                it.isClickable = false
            }
            1 -> binding.saveTimeSwitch.let {
                it.isChecked = true
                it.isClickable = false
            }
            2 -> binding.saveVoiceSwitch.let {
                it.isChecked = true
                it.isClickable = false
            }
            else -> {
                Log.e(TAG, "error settings ${yuyinViewModel.settings} ")
                yuyinViewModel.settings.saveMode = 0
                binding.saveTextSwitch.let {
                    it.isChecked = true
                    it.isClickable = false
                }
            }
        }
        binding.saveTextSwitch.setOnCheckedChangeListener(switchListener(0))
        binding.saveTimeSwitch.setOnCheckedChangeListener(switchListener(1))
        binding.saveVoiceSwitch.setOnCheckedChangeListener(switchListener(2))
    }

    private fun initSpinner() {
        // 声明一个下拉列表的数组适配器
        modelKeys = yuyinViewModel.newSettings.model_dict.keys.toMutableList()
        val position = modelKeys.indexOf(yuyinViewModel.newSettings.modelMode)
        val startAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.item_select,
            modelKeys
        )
        binding.dictPathText.text = File(yuyinViewModel.dicPath).name
        binding.modePathText.text = File(yuyinViewModel.modelPath).name
        // 设置数组适配器的布局样式
        startAdapter.setDropDownViewResource(R.layout.item_drapdown)
        // 下拉框
        binding.spinnerModel.let { sp ->
            // 设置下拉框标题
            sp.prompt = "Model:"
            sp.adapter = startAdapter
            sp.setSelection(position, false)
            sp.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectMode = modelKeys[position]
                    yuyinViewModel.newSettings.modelMode = selectMode
                    if (selectMode == "zh" || selectMode == "en") {
                        // 默认mode不需要更改
                        binding.chooseDict.isEnabled = false
                        binding.chooseModel.isEnabled = false
                        // 初始化
                        newDictUri = null
                        newModelUri = null
                    } else {
                        binding.chooseDict.isEnabled = true
                        binding.chooseModel.isEnabled = true
                    }
                    binding.dictPathText.text = File(yuyinViewModel.newSettings.dictPath()).name
                    binding.modePathText.text = File(yuyinViewModel.newSettings.modelPath()).name
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
        }
    }

    private fun initPickFileButton() {
        binding.chooseModel.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                /**
                 * It's possible to limit the types of files by mime-type. Since this
                 * app displays pages from a PDF file, we'll specify `application/pdf`
                 * in `type`.
                 * See [Intent.setType] for more details.
                 */
                type = MainActivityView.zipMineType
                /**
                 * Because we'll want to use [ContentResolver.openFileDescriptor] to read
                 * the data of whatever file is picked, we set [Intent.CATEGORY_OPENABLE]
                 * to ensure this will succeed.
                 */
                addCategory(Intent.CATEGORY_OPENABLE)

            }
            openDocumentPickerForModel.launch(intent)
        }
        binding.chooseDict.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = MainActivityView.textMineType
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            openDocumentPickerForDict.launch(intent)
        }
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
                        val dialog =
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.save_settings)
                                .setNegativeButton(R.string.cancel) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(R.string.confirm) { _, _ ->
                                    // 更新配置
                                    val jsonAdapter: JsonAdapter<LocalSettings> =
                                        yuyinViewModel.moshi.adapter(LocalSettings::class.java)
                                    if (yuyinViewModel.newSettings.modelMode == "自定义") {
                                        if (newDictUri == null) {
                                            Toast.makeText(
                                                requireContext(),
                                                "you should choose a new dict",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else if (newModelUri == null) {
                                            Toast.makeText(
                                                requireContext(),
                                                "you should choose a new model",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            yuyinViewModel.viewModelScope.launch(Dispatchers.IO) {
                                                yuyinViewModel.newSettings.modelPath().let {
                                                    val tmp = File(it)
                                                    if (tmp.exists()) {
                                                        tmp.delete()
                                                    }
                                                }
                                                yuyinViewModel.newSettings.dictPath().let {
                                                    val tmp = File(it)
                                                    if (tmp.exists()) {
                                                        tmp.delete()
                                                    }
                                                }
                                                val fileDict = Paths.get(
                                                    yuyinViewModel.yuYinDirPath.absolutePathString(),
                                                    binding.dictPathText.text.toString()
                                                ).toFile()
                                                if (!fileDict.exists()) {
                                                    requireActivity().contentResolver.openInputStream(
                                                        newDictUri!!
                                                    ).use { inputStream ->
                                                        fileDict.outputStream().use {
                                                            inputStream?.copyTo(it)
                                                        }
                                                    }
                                                }
                                                val fileModel = Paths.get(
                                                    yuyinViewModel.yuYinDirPath.absolutePathString(),
                                                    binding.modePathText.text.toString()
                                                ).toFile()
                                                if (!fileModel.exists()) {
                                                    requireActivity().contentResolver.openInputStream(
                                                        newModelUri!!
                                                    ).use { inputStream ->
                                                        fileModel.outputStream().use {
                                                            inputStream?.copyTo(it)
                                                        }
                                                    }
                                                }
                                                yuyinViewModel.newSettings.model_dict[yuyinViewModel.newSettings.modelMode] =
                                                    mutableListOf(
                                                        fileModel.absolutePath,
                                                        fileDict.absolutePath
                                                    )
                                                val json =
                                                    jsonAdapter.toJson(yuyinViewModel.newSettings)
                                                yuyinViewModel.settingProfilePath.toFile()
                                                    .writeText(json)
                                                yuyinViewModel.settings.modelMode =
                                                    yuyinViewModel.newSettings.modelMode
                                                yuyinViewModel.settings =
                                                    yuyinViewModel.newSettings.copy()
                                            }
                                        }
                                    } else {
                                        // just copy
                                        val json =
                                            jsonAdapter.toJson(yuyinViewModel.newSettings)
                                        yuyinViewModel.settingProfilePath.toFile()
                                            .writeText(json)
                                        yuyinViewModel.settings =
                                            yuyinViewModel.newSettings.copy()
                                    }
                                }.create()
                        dialog.show()
                        true
                    }
                    else -> {
                        false
                    }
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun enableSwitch(flag: Boolean) {
        binding.saveVoiceSwitch.isClickable = flag
        binding.saveTextSwitch.isClickable = flag
        binding.saveTimeSwitch.isClickable = flag
    }

    private fun checkSwitch(flag: Boolean) {
        binding.saveVoiceSwitch.isChecked = flag
        binding.saveTextSwitch.isChecked = flag
        binding.saveTimeSwitch.isChecked = flag
    }

    private fun switchListener(saveMode:Int) = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        if (isChecked) {
            checkSwitch(false)
            enableSwitch(true)
            buttonView.isChecked = true
            buttonView.isClickable = false
            yuyinViewModel.newSettings.saveMode = saveMode
        }
    }

}