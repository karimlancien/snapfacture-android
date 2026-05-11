package com.ohmybattery.invoicing.core.pdf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareInvoice {

    fun intent(context: Context, file: File, invoiceNumber: Int): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Facture Ohmybattery N° $invoiceNumber")
            putExtra(Intent.EXTRA_TEXT, "Bonjour,\n\nVeuillez trouver ci-joint votre facture N° $invoiceNumber.\n\nCordialement,\nOhmybattery")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Partager la facture").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
