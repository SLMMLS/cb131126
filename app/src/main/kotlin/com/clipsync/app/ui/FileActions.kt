package com.clipsync.app.ui

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clipsync.app.files.FileNames
import com.clipsync.app.files.PublicDownloads
import com.clipsync.app.files.TransferView
import java.io.File

/*
 * What can be done with a file that came from the PC. Each function returns a sentence for the person when it
 * cannot do it (or null when it did), so the screen only has to show it.
 */

/**
 * Opens a received file, asking which app to use ("Открыть с помощью": the system's own list, every time, as the plan
 * for the "Файлы" tab says). Programs and installers are never opened from here.
 */
internal fun openReceived(context: Context, view: TransferView): String? {
    val location = view.location ?: return "Файл не найден"
    if (FileNames.isRisky(view.name)) {
        return "ClipSync не открывает программы и установочные файлы, пришедшие по сети. Откройте «Загрузки» и решите сами."
    }
    if (!location.startsWith("content://")) {
        // Android 9 keeps received files in the app's own folder, which other apps cannot read.
        return "На этой версии Android файл можно открыть только из приложения «Файлы»: ${PublicDownloads.whereToLook()}"
    }
    val viewIntent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(location), FileNames.mimeType(view.name))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // createChooser carries the read grant over to the app the person picks (it copies the flag and the address into
    // the chooser's own clip), so a viewer that has no right to the file on its own can still read it.
    return try {
        context.startActivity(Intent.createChooser(viewIntent, "Открыть с помощью").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (_: ActivityNotFoundException) {
        "На телефоне нет приложения, которое открывает такие файлы"
    } catch (_: SecurityException) {
        "Файл больше недоступен: его переместили или удалили"
    }
}

/**
 * "Поделиться": hands the file to another app through the system's share sheet. Sharing is the person's own decision
 * and runs nothing (unlike "open"), so it is offered for every kind of file.
 */
internal fun shareReceived(context: Context, view: TransferView): String? {
    val location = view.location ?: return "Файл не найден"
    if (!location.startsWith("content://")) {
        return "На этой версии Android поделиться файлом отсюда нельзя: откройте «Загрузки» и сделайте это там"
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType(FileNames.mimeType(view.name))
        .putExtra(Intent.EXTRA_STREAM, Uri.parse(location))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(Intent.createChooser(send, "Поделиться файлом").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (_: ActivityNotFoundException) {
        "На телефоне нет приложения, которому можно передать этот файл"
    } catch (_: SecurityException) {
        "Файл больше недоступен: его переместили или удалили"
    }
}

internal fun openDownloads(context: Context): String? =
    try {
        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (_: Exception) {
        "Откройте приложение «Файлы» и папку ${PublicDownloads.whereToLook()}"
    }

internal sealed interface DeleteResult {
    data object Deleted : DeleteResult

    /** Nothing to delete: the person already removed or moved it. The row can go. */
    data object AlreadyGone : DeleteResult

    data class Failed(val sentence: String) : DeleteResult
}

/**
 * "Удалить файл": removes the received copy from the phone (the one on the PC is not touched). A file that ClipSync
 * saved itself can be deleted without asking Android; if the system refuses (the file was saved by an earlier
 * installation of the app, say), the person is told where to delete it by hand.
 */
internal fun deleteReceived(context: Context, view: TransferView): DeleteResult {
    val location = view.location ?: return DeleteResult.AlreadyGone
    return try {
        val removed = if (location.startsWith("content://")) {
            context.contentResolver.delete(Uri.parse(location), null, null) > 0
        } else {
            File(location).delete()
        }
        if (removed) DeleteResult.Deleted else DeleteResult.AlreadyGone
    } catch (_: SecurityException) {
        DeleteResult.Failed("Android не разрешает ClipSync удалить этот файл (он мог быть сохранён прежней установкой). Удалите его в приложении «Файлы»: ${PublicDownloads.whereToLook()}")
    } catch (_: Exception) {
        DeleteResult.Failed("Не удалось удалить файл")
    }
}
