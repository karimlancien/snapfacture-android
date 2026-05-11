package com.ohmybattery.invoicing.data.local

import androidx.room.TypeConverter
import com.ohmybattery.invoicing.data.local.entity.InvoiceStatus
import com.ohmybattery.invoicing.data.local.entity.PaymentMethod

class RoomConverters {
    @TypeConverter fun statusToString(s: InvoiceStatus): String = s.name
    @TypeConverter fun statusFromString(s: String): InvoiceStatus = InvoiceStatus.valueOf(s)

    @TypeConverter fun paymentToString(p: PaymentMethod): String = p.name
    @TypeConverter fun paymentFromString(s: String): PaymentMethod = PaymentMethod.valueOf(s)
}
