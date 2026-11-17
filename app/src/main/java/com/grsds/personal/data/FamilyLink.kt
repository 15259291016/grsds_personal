package com.grsds.personal.data

/**
 * 家庭成员与专项附加扣除的联动：
 * 在扣除表单里直接挑一位已维护的成员，把姓名、证件号等带进表单，避免重复录入。
 *
 * 这里的映射关系（哪个扣除项目能选哪种关系、带哪些字段）属于业务规则，
 * 放在数据层便于单测，不散落在 Compose 代码里。
 */
object FamilyLink {

    /** 各扣除项目可选的成员关系。 */
    private val candidatesByType: Map<String, Set<String>> = mapOf(
        "子女教育" to setOf("子女"),
        "3岁以下婴幼儿照护" to setOf("子女"),
        "赡养老人" to setOf("父亲", "母亲", "父母", "祖父母", "外祖父母"),
        "大病医疗" to setOf("配偶", "子女", "父亲", "母亲", "父母")
    )

    /** 该扣除项目是否支持从家庭成员带入。 */
    fun supports(type: String): Boolean = candidatesByType.containsKey(type)

    /** 可带入该扣除项目的成员，保持成员列表原有顺序。 */
    fun candidates(type: String, members: List<FamilyMember>): List<FamilyMember> {
        val relations = candidatesByType[type] ?: return emptyList()
        return members.filter { it.relation in relations }
    }

    /** 选中成员后要回填的表单字段，值为空串的字段会被略过。 */
    fun prefill(type: String, member: FamilyMember): Map<String, String> {
        val raw = when (type) {
            "子女教育" -> mapOf(
                "child_name" to member.name,
                "child_id" to member.idNumber,
                "child_birth" to member.birthDateLabel
            )
            "3岁以下婴幼儿照护" -> mapOf(
                "baby_name" to member.name,
                "baby_id" to member.idNumber,
                "baby_birth" to member.birthDateLabel
            )
            "赡养老人" -> mapOf(
                "elder_name" to member.name,
                "elder_id" to member.idNumber,
                "elder_relation" to elderRelationOf(member.relation)
            )
            "大病医疗" -> mapOf(
                "patient_name" to member.name,
                "relation" to medicalRelationOf(member.relation)
            )
            else -> emptyMap()
        }
        return raw.filterValues { it.isNotBlank() }
    }

    /** 被赡养人与纳税人的关系，取值需与赡养老人表单的候选项一致。 */
    fun elderRelationOf(relation: String): String = when (relation) {
        "父亲" -> "父亲"
        "母亲" -> "母亲"
        else -> "其他法定赡养人"
    }

    /** 大病医疗患者的身份，取值需与医疗表单的候选项一致。 */
    fun medicalRelationOf(relation: String): String = when (relation) {
        "配偶" -> "配偶"
        "子女" -> "未成年子女"
        "父亲", "母亲", "父母" -> "父母"
        else -> "其他"
    }
}
