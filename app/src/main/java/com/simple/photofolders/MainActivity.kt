package com.simple.photofolders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.simple.photofolders.ui.BusyDialog
import com.simple.photofolders.ui.FolderDetailScreen
import com.simple.photofolders.ui.FolderListScreen
import com.simple.photofolders.ui.FoldersViewModel
import com.simple.photofolders.ui.PhotoViewerScreen
import com.simple.photofolders.ui.theme.PhotoFoldersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoFoldersTheme {
                App()
            }
        }
    }
}

@Composable
private fun App(vm: FoldersViewModel = viewModel()) {
    val nav = rememberNavController()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val currentFolder by vm.currentFolder.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    NavHost(navController = nav, startDestination = "folders") {
        composable("folders") {
            FolderListScreen(
                folders = folders,
                onOpenFolder = { id -> nav.navigate("folder/$id") },
                onCreate = vm::createFolderWithPhotos,
                onRename = vm::renameFolder,
                onDelete = vm::deleteFolder,
            )
        }

        composable(
            "folder/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            LaunchedEffect(id) { vm.openFolder(id) }
            val loaded = currentFolder?.id == id
            FolderDetailScreen(
                folder = currentFolder.takeIf { loaded },
                photos = if (loaded) photos else emptyList(),
                onBack = { nav.popBackStack() },
                onOpenPhoto = { index -> nav.navigate("viewer/$id/$index") },
                onAddPhotos = { uris -> vm.addPhotos(id, uris) },
                onDeletePhotos = { files -> vm.deletePhotos(id, files) },
            )
        }

        composable(
            "viewer/{id}/{index}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("index") { type = NavType.IntType },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val index = entry.arguments?.getInt("index") ?: 0
            LaunchedEffect(id) { vm.openFolder(id) }
            if (currentFolder?.id != id) {
                // Still loading (e.g. after app was restored).
                Box(Modifier.fillMaxSize().background(Color.Black))
            } else {
                PhotoViewerScreen(
                    photos = photos,
                    startIndex = index,
                    onBack = { nav.popBackStack() },
                    onDelete = { file -> vm.deletePhotos(id, listOf(file)) },
                )
            }
        }
    }

    if (busy) BusyDialog()
}
