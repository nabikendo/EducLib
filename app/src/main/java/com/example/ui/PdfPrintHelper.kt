package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.BookEntity
import com.example.data.BorrowerEntity
import com.example.data.LoanEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilePrintDocumentAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null

        try {
            input = FileInputStream(file)
            output = FileOutputStream(destination?.fileDescriptor)

            val buf = ByteArray(1024)
            var bytesRead: Int
            while (input.read(buf).also { bytesRead = it } > 0) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                output.write(buf, 0, bytesRead)
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        } finally {
            try {
                input?.close()
                output?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

object PdfPrintHelper {

    fun generateLoanReceiptPdf(
        context: Context,
        loan: LoanEntity,
        book: BookEntity?,
        borrower: BorrowerEntity?
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Palette matching modern styles
        val primaryColor = Color.parseColor("#4A148C")
        val secondaryColor = Color.parseColor("#7B1FA2")
        val textColor = Color.parseColor("#212121")
        val lightGray = Color.parseColor("#F5F5F5")
        val lineGray = Color.parseColor("#DEDEDE")
        val overdueRed = Color.parseColor("#D32F2F")
        val availableGreen = Color.parseColor("#388E3C")

        val paint = Paint().apply { isAntiAlias = true }
        
        // Brand Header Background Rectangle
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, 595f, 120f, paint)

        // White typography inside header
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BIBLIOTECH", 40f, 55f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Comprovante Oficial de Empréstimo", 40f, 75f, paint)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(loan.loanDate))
        
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("ID: EMP-${String.format("%05d", loan.id)}", 430f, 45f, paint)
        canvas.drawText("Emitido: $dateStr", 390f, 65f, paint)

        var currentY = 160f

        // Borrower Profile Section Card
        paint.color = lightGray
        canvas.drawRoundRect(35f, currentY, 560f, currentY + 115f, 8f, 8f, paint)

        paint.color = secondaryColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DADOS DO LEITOR / ESTUDANTE", 50f, currentY + 25f, paint)

        paint.color = textColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Nome:", 50f, currentY + 50f, paint)
        canvas.drawText("Vínculo:", 50f, currentY + 70f, paint)
        canvas.drawText("Turma/Contato:", 50f, currentY + 90f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(borrower?.name ?: loan.borrowerName, 170f, currentY + 50f, paint)
        canvas.drawText(borrower?.type ?: "Aluno", 170f, currentY + 70f, paint)
        canvas.drawText(borrower?.classRoom?.ifBlank { loan.borrowerClass } ?: loan.borrowerClass, 170f, currentY + 90f, paint)
        
        if (borrower?.identifier != null && borrower.identifier.isNotEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Matrícula/ID:", 350f, currentY + 70f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(borrower.identifier, 430f, currentY + 70f, paint)
        }
        if (borrower?.phone != null && borrower.phone.isNotEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Contato:", 350f, currentY + 90f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(borrower.phone, 430f, currentY + 90f, paint)
        }

        // Book details Section
        currentY += 140f
        paint.color = secondaryColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DADOS DO LIVRO EMPRESTADO", 50f, currentY, paint)

        paint.color = lineGray
        paint.strokeWidth = 1f
        canvas.drawLine(35f, currentY + 8f, 560f, currentY + 8f, paint)

        currentY += 30f
        paint.color = textColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Título do Livro:", 50f, currentY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(book?.title ?: loan.bookTitle, 150f, currentY, paint)

        currentY += 20f
        paint.color = textColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Autor / Escritor:", 50f, currentY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(book?.author ?: "Não informado", 150f, currentY, paint)

        currentY += 20f
        paint.color = textColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Gênero Literário:", 50f, currentY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(book?.genre ?: "Não informado", 150f, currentY, paint)

        currentY += 20f
        paint.color = textColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Cod. Item / Local:", 50f, currentY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val shelfLocation = book?.location ?: "Geral"
        canvas.drawText(shelfLocation, 150f, currentY, paint)

        // Loan detail deadlines
        currentY += 45f
        paint.color = secondaryColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRAZOS E SITUAÇÃO DO EMPRÉSTIMO", 50f, currentY, paint)

        paint.color = lineGray
        canvas.drawLine(35f, currentY + 8f, 560f, currentY + 8f, paint)

        currentY += 30f
        paint.color = textColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Data de Retirada:", 50f, currentY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val checkoutStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(loan.loanDate))
        canvas.drawText(checkoutStr, 170f, currentY, paint)

        currentY += 20f
        paint.color = textColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Devolver Até (Prazo):", 50f, currentY, paint)
        val isOverdue = loan.returnDate == null && loan.calculatedIsOverdue()
        paint.color = if (isOverdue) overdueRed else primaryColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val returnByStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(loan.dueDate))
        canvas.drawText("$returnByStr ${if (isOverdue) "(EM ATRASO)" else ""}", 170f, currentY, paint)

        currentY += 20f
        paint.color = textColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Situação do Vínculo:", 50f, currentY, paint)
        
        val statusText: String
        val statusColor: Int
        if (loan.returnDate != null) {
            val returnStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(loan.returnDate!!))
            statusText = "Devolvido em $returnStr"
            statusColor = availableGreen
        } else if (isOverdue) {
            statusText = "Atrasado (Pendente de Devolução)"
            statusColor = overdueRed
        } else {
            statusText = "Em Aberto (Regularizado)"
            statusColor = primaryColor
        }
        
        paint.color = statusColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(statusText, 170f, currentY, paint)

        // Signatures at bottom
        currentY = 640f
        paint.color = lineGray
        canvas.drawLine(50f, currentY, 240f, currentY, paint)
        canvas.drawLine(350f, currentY, 540f, currentY, paint)

        paint.color = textColor
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Assinatura do Estudante/Leitor", 85f, currentY + 15f, paint)
        canvas.drawText("Assinatura do Bibliotecário", 390f, currentY + 15f, paint)

        // Lower warnings banner
        currentY = 740f
        paint.color = secondaryColor
        paint.strokeWidth = 2f
        canvas.drawLine(35f, currentY, 560f, currentY, paint)

        paint.color = Color.parseColor("#757575")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        
        val warningLine1 = "Atenção: A conservação dos livros do colégio é de responsabilidade de todos."
        val warningLine2 = "A devolução em atraso acarreta suspensão temporária e bloqueia novos empréstimos de livros."
        val warningLine3 = "Por favor, colabore respeitando as datas limites. Excelente leitura e bons estudos!"

        canvas.drawText(warningLine1, 35f, currentY + 20f, paint)
        canvas.drawText(warningLine2, 35f, currentY + 32f, paint)
        canvas.drawText(warningLine3, 35f, currentY + 44f, paint)

        pdfDocument.finishPage(page)
        
        val file = File(context.cacheDir, "comprovante_emprestimo_${loan.id}.pdf")
        return try {
            val out = FileOutputStream(file)
            pdfDocument.writeTo(out)
            pdfDocument.close()
            out.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun printLoanPdf(context: Context, loan: LoanEntity, book: BookEntity?, borrower: BorrowerEntity?) {
        val file = generateLoanReceiptPdf(context, loan, book, borrower)
        if (file != null) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "BiblioTech_Comprovante_${loan.id}"
            val printAdapter = FilePrintDocumentAdapter(file)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }

    fun shareLoanPdf(context: Context, loan: LoanEntity, book: BookEntity?, borrower: BorrowerEntity?): android.content.Intent? {
        val file = generateLoanReceiptPdf(context, loan, book, borrower) ?: return null
        return try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Comprovante de Empréstimo - BiblioTech")
                putExtra(android.content.Intent.EXTRA_TEXT, "Segue em anexo o comprovante de empréstimo do livro \"${loan.bookTitle}\" em formato PDF.")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
