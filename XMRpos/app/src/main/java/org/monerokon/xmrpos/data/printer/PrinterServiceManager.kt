package org.monerokon.xmrpos.data.printer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import net.nyx.printerservice.print.IPrinterService
import net.nyx.printerservice.print.PrintTextFormat

class PrinterServiceManager(private val context: Context) {

    private var printerService: IPrinterService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            printerService = IPrinterService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            printerService = null
        }
    }

    fun bindPrinterService() {
        val intent = Intent().apply {
            setPackage("net.nyx.printerservice")
            action = "net.nyx.printerservice.IPrinterService"
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindPrinterService() {
        context.unbindService(serviceConnection)
    }

    fun printText(text: String) {
        printerService?.let {
            val format = PrintTextFormat()
            // Set formatting options for text here (optional)
            it.printText(text, format)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }

    fun printTextCenter(text: String) {
        printerService?.let {
            val format = PrintTextFormat().apply {
                ali = 1
            }
            it.printText(text, format)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }

    fun printSpace() {
        printerService?.let {
            val format = PrintTextFormat().apply {
                topPadding = 10
            }
            it.printText("", format)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }

    fun printSpacer() {
        printerService?.let {
            val format = PrintTextFormat().apply {
                ali = 1
                topPadding = 5
            }
            it.printText("------------------------------------------------", format)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }
}
