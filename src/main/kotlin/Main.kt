import com.google.gson.Gson
import java.io.File

private const val VERSION = "1.0.0"
private val allMap = HashMap<String, HashMap<String, String>>()
private val valueBindModuleMap = HashMap<String, String>()
fun main(args: Array<String>) {
    println("作者: shuike    版本:$VERSION\n")
    try {
        val runtime = Runtime.getRuntime()
        val exec = runtime.exec("git branch")
        val string = String(exec.inputStream.readAllBytes()).trim()
        if (string.isNotEmpty()) {
            val split = string.split("\n")
            val currentBranch = split.findLast { it.trim().startsWith("*") }
            if (currentBranch != null) {
                val branch = currentBranch.substring(currentBranch.indexOf("*") + 1, currentBranch.length).trim()
                if (branch in arrayOf("main", "master")) {
                    println("当前分支为:${branch}\n此分支可能为主分支，为了保证工程数据安全性，请勿在主分支进行工具的运行。")
                    return
                }
            }
        }
    } catch (_: Exception) {
        // git检测失败也没有关系，继续执行即可
    }

    if (args.size < 2) {
        println("参数不正确，格式为: 项目路径 json数据路径")
        return
    }
    val androidProjectPath = args[0]
    val jsonDataFilePath = args[1]
    val file = File(androidProjectPath)
    if (!file.exists()) {
        println("工程文件不存在。路径:${androidProjectPath}")
        return
    }
    val jsonDataFile = File(jsonDataFilePath)
    if (!jsonDataFile.exists()) {
        println("json语言数据文件不存在。路径:${androidProjectPath}")
        return
    }
    val jsonData = jsonDataFile.readText()

    prepareValueBindMap(file)
    val jsonBean = Gson().fromJson(jsonData, JsonBean::class.java)
    var processNum = 0
    jsonBean.forEach { itemBean ->
        val targetContent = itemBean.target_content
        val mapValue = valueBindModuleMap[targetContent]
        itemBean.languages.forEach { languageBean ->
            if (mapValue != null) {
                val language = languageBean.language
                val content = languageBean.content.replace("'", "\\'")
                val module = mapValue.substring(0, mapValue.indexOf(":"))
                val s = "${androidProjectPath}/${module}/src/main/res/values-${language}/strings.xml"
                val stringsFile = File(s)
                val keys = mapValue.substring(mapValue.indexOf(":") + 1, mapValue.length).split(",")
                if (!stringsFile.exists()) { // 文件不存在
                    println("不存在 ${s}")
                    val parentFile = stringsFile.parentFile
                    if (!parentFile.exists()) {
                        parentFile.mkdirs()
                    }
                    var text = "<?xml version=\"1.0\" ?>\n<resources>"
                    keys.forEach { key ->
                        text += "\n\t<string name=\"${key}\">$content</string>"
                    }
                    text += "\n</resources>"
                    stringsFile.writeText(text)
                } else {
                    val setedKeys = HashSet<String>()
                    var str = ""
                    stringsFile.forEachLine { line ->
                        if (line.startsWith("<?xml version=\"1.0\" ?><resources>")) {
                            str += line.replace(
                                "<?xml version=\"1.0\" ?><resources>", "<?xml version=\"1.0\" ?>\n<resources>"
                            )
                        } else {
                            val regex = Regex("name=\"(\\w+)\"")
                            val valueRegex = Regex(">(.*?)</")
                            val find = regex.find(line)
                            val find2 = valueRegex.find(line)
                            if (find != null) {
                                val lineKey = find.groups[1]!!.value
                                val value = find2!!.groups[1]!!.value
                                str += if (lineKey in keys) {
                                    setedKeys.add(lineKey)
                                    if (value != content) {
                                        processNum++
                                        "\n".plus(line.replace(valueRegex, ">$content</"))
                                    } else {
                                        "\n".plus(line)
                                    }
                                } else {
                                    "\n".plus(line)
                                }
                            } else {
                                str += "\n".plus(line)
                            }
                        }
                    }
                    str = str.replace("</resources>", "")
                    keys.forEach { key ->
                        if (!setedKeys.contains(key)) {
                            processNum++
                            str += "\t<string name=\"${key}\">$content</string>\n"
                        }
                    }
                    if (!str.endsWith("\n")) {
                        str += "\n"
                    }
                    str = str.plus("</resources>")
                    stringsFile.writeText(str.trim())
                }
            }
        }
    }
    if (processNum == 0) {
        println("没有需要处理的新数据")
    } else {
        println("数据处理完成!")
    }
//    println(GsonBuilder().disableHtmlEscaping().create().toJson(allMap))
}

private fun prepareValueBindMap(file: File) {
    val walk = file.walk()
    val sequence = walk.filter {
        it.isFile && it.absolutePath.contains("/values/") && it.name.equals("strings.xml")
    }
    sequence.forEach {
        val replace = it.absolutePath.replace(file.absolutePath + "/", "")
        // 模块名
        val moduleName = replace.substring(0, replace.indexOf("/"))
        val map = HashMap<String, String>()
        it.forEachLine {
            val regex = Regex("name=\"(\\w+)\"")
            val valueRegex = Regex(">(.*?)</")
            val find = regex.find(it)
            val find2 = valueRegex.find(it)
            find?.let {
                val key = it.groups[1]!!.value
                val value = find2!!.groups[1]!!.value
                map[key] = value
                val s = valueBindModuleMap[value]
                if (s != null) {
                    valueBindModuleMap[value] = valueBindModuleMap[value].plus(",").plus(key)
                } else {
                    valueBindModuleMap[value] = "${moduleName}:$key"
                }
            }
        }
        allMap[moduleName] = map
    }
}