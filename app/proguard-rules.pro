# Reglas de R8 para CampusPocket.
# Room, Hilt y Compose incluyen sus propias reglas de consumidor; no hace falta mantener todo el paquete.
# Cuando se agreguen librerías (PdfBox-Android, etc.) en fases posteriores, añadir aquí solo lo que pidan.

# kotlinx.serialization: conservar los serializadores generados del respaldo JSON,
# que se resuelven por reflexión en tiempo de ejecución.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.campuspocket.**$$serializer { *; }
-keepclassmembers class com.example.campuspocket.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.campuspocket.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# PDFBox referencia un decodificador JPEG2000 opcional que no se empaqueta;
# solo se usa si el PDF trae imágenes JPX, que el importador no procesa.
-dontwarn com.gemalto.jp2.JP2Decoder
