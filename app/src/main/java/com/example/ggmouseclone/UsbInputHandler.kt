package com.example.ggmouseclone

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.util.Log
import java.util.concurrent.Executors

class UsbInputHandler(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var device: UsbDevice? = null
    private var endpointIn: UsbEndpoint? = null
    private var connection: android.hardware.usb.UsbDeviceConnection? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var running = false
    private var onKeyListener: ((Int) -> Unit)? = null

    fun setOnKeyListener(listener: (Int) -> Unit) {
        this.onKeyListener = listener
    }

    fun start() {
        for (dev in usbManager.deviceList.values) {
            if (dev.deviceClass == UsbConstants.USB_CLASS_HID) {
                device = dev
                break
            }
        }
        device?.let {
            val intf = it.getInterface(0)
            for (i in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(i)
                if (ep.direction == UsbConstants.USB_DIR_IN) {
                    endpointIn = ep
                }
            }
            connection = usbManager.openDevice(it)
            connection?.claimInterface(intf, true)
            running = true
            executor.execute(readLoop)
        }
    }

    private val readLoop = Runnable {
        val buffer = ByteArray(8)
        while (running) {
            val ep = endpointIn ?: continue
            val conn = connection ?: continue
            val len = conn.bulkTransfer(ep, buffer, buffer.size, 100)
            if (len > 0) {
                val keyCode = buffer[2].toInt() and 0xFF
                if (keyCode != 0) {
                    onKeyListener?.invoke(keyCode)
                }
            }
        }
    }

    fun stop() {
        running = false
        connection?.close()
        executor.shutdown()
    }
}
