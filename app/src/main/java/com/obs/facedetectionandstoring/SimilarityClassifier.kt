package com.obs.facedetectionandstoring

interface SimilarityClassifier {
    class Recognition(
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        var id: String?,
        /** Display name for the recognition.  */
        var title: String?,
        var distance: Float?,

    ) {
        var extra: Any?

        init {
            id = id
            title = title
            distance = distance
            this.extra = null
        }

        @JvmName("setExtra1")
        fun setExtra(extra: Any?) {
            this.extra = extra
        }

        @JvmName("getExtra1")
        fun getExtra(): Any? {
            return extra
        }


        override fun toString(): String {
            var resultString = ""
            if (id != null) {
                resultString += "[$id] "
            }
            if (title != null) {
                resultString += "$title "
            }
            if (distance != null) {
                resultString += String.format("(%.1f%%) ", distance!! * 100.0f)
            }
            return resultString.trim { it <= ' ' }
        }
    }

}