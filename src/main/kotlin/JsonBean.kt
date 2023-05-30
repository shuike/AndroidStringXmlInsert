class JsonBean : ArrayList<AItem>()

data class AItem(
    val languages: List<Language>,
    val target_content: String
)

data class Language(
    val content: String,
    val language: String
)