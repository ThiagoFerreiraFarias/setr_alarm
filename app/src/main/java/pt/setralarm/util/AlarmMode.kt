package pt.setralarm.util

enum class AlarmMode(val status: String) {
    DISARMED("Disarming System\r"),
    ARMED_FULL("Arming HOME System\r"),
    ARMED_IN_HOME("Arming HOME System\r"),
    UNKNOWN("unknown");

    fun getRequestMessage() : String{
        return when(this){
            DISARMED -> "!mode DIS "
            ARMED_FULL -> "!mode ARM "
            ARMED_IN_HOME -> "!mode HOME "
            else -> ""
        }
    }

}