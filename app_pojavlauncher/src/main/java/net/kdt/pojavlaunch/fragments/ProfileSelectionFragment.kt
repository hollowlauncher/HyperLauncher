package net.kdt.pojavlaunch.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi
import net.kdt.pojavlaunch.ui.screens.ProfileSelectionScreen
import net.kdt.pojavlaunch.kotlin.ui.viewmodel.ProfileSelectionViewModel
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProfileSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val viewModel: ProfileSelectionViewModel = viewModel()
                val context = LocalContext.current

                val modpackApi = remember { CommonApi(context.getString(R.string.curseforge_api_key)) }
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    uri?.let {
                        val contentResolver = context.contentResolver
                        PojavApplication.sExecutorService.execute {
                            val fileName = Tools.getFileName(context, it) ?: "modpack"
                            val outFile = File(Tools.DIR_CACHE, "$fileName.cf")
                            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.multirt_progress_caching)
                            try {
                                contentResolver.openInputStream(it)?.use { input ->
                                    FileOutputStream(outFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            } catch (e: IOException) {
                                Tools.showErrorRemote("Error", e)
                                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                                return@execute
                            }
                            try {
                                modpackApi.installLocalModpack(fileName, outFile, null)
                                viewModel.loadProfiles()
                            } catch (e: IOException) {
                                Tools.showErrorRemote("Error", e)
                            } finally {
                                outFile.delete()
                                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.loadProfiles()
                }

                PojavTheme {
                    ProfileSelectionScreen(
                        onImportClick = {
                            importLauncher.launch("*/*")
                        },
                        onCreateClick = {
                            Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment::class.java, ProfileTypeSelectFragment.TAG, null)
                        },
                        onSelectDirClick = {

                        },
                        onEditClick = { instance ->

                            viewModel.selectInstance(instance)
                            Tools.swapFragment(requireActivity(), InstanceEditorFragment::class.java, InstanceEditorFragment.TAG, null)
                        },
                        onDeleteClick = { instance ->
                            viewModel.deleteInstance(instance) {}
                        },
                        onSelect = { instance ->
                            viewModel.selectInstance(instance)
                            Tools.backToMainMenu(requireActivity())
                        },
                        onFilterChange = { r, s, m -> viewModel.updateFilters(r, s, m) },
                        onSearchModClick = {
                            Tools.swapFragment(requireActivity(), SearchModFragment::class.java, SearchModFragment.TAG, null)
                        },
                        profiles = viewModel.filteredList,
                        selectedPathName = viewModel.selectedInstancePathName,
                        showReleases = viewModel.showReleases,
                        showSnapshots = viewModel.showSnapshots,
                        showModded = viewModel.showModded,
                        isLoading = viewModel.isLoading
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "ProfileSelectionFragment"
    }
}
