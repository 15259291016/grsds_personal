package com.grsds.personal.data

import kotlinx.serialization.Serializable

/** 一条专项附加扣除填报记录，金额以“分”存储。 */
@Serializable
data class DeductionRecord(
    val type: String,
    val year: Int,
    val ratioPercent: Int,
    val amountInCents: Long,
    val updatedAt: Long,
    /** 分项表单填写的信息，键见 [DEDUCTION_FIELDS]。旧记录为空表。 */
    val details: Map<String, String> = emptyMap()
) {
    /** 表单里填过的字段数，列表上用来提示“已填写 N 项信息”。 */
    val filledDetailCount: Int
        get() = details.count { it.value.isNotBlank() }
}

/** 登录会话，演示版只保存手机号与昵称，不做真实鉴权。 */
@Serializable
data class LoginSession(
    val phone: String,
    val nickname: String,
    val loginAt: Long
) {
    /** 138****8888 形式的脱敏手机号。 */
    val maskedPhone: String
        get() = if (phone.length == 11) "${phone.take(3)}****${phone.takeLast(4)}" else phone
}

/**
 * 专项附加扣除项目定义：名称、年度扣除标准（分）、是否支持 50% 分摊、图标字与配色。
 *
 * [DeductionOption.type] 会随填报记录一起落盘，属于数据值而非纯 UI 文案，
 * 因此与 [INCOME_TYPE_OPTIONS] 一样保留字面量、不抽到 strings.xml。
 */
data class DeductionOption(
    val type: String,
    val yearlyStandardInCents: Long,
    val supportsHalf: Boolean,
    val symbol: String,
    val colorHex: Long,
    /** 大病医疗按实际自付金额据实扣除，不按年度标准定额。 */
    val actualBased: Boolean = false,
    /** 口语化的扣除口径说明，填报弹层顶部展示。 */
    val ruleHint: String = ""
)

val DEDUCTION_OPTIONS = listOf(
    DeductionOption("子女教育", 24_000_00, true, "子", 0xFF1598F4, ruleHint = "每个子女每月 2000 元，父母可各扣 50%"),
    DeductionOption("继续教育", 4_800_00, false, "续", 0xFF8B5CF6, ruleHint = "学历（学位）继续教育每月 400 元，职业资格按年 3600 元"),
    DeductionOption(
        "大病医疗", 80_000_00, false, "病", 0xFFFF7043,
        actualBased = true,
        ruleHint = "医保目录内个人自付超过 15000 元的部分据实扣除，年度限额 80000 元"
    ),
    DeductionOption("住房贷款利息", 12_000_00, true, "贷", 0xFF3D8BFF, ruleHint = "首套住房贷款利息每月 1000 元，夫妻可约定一方扣除"),
    DeductionOption("住房租金", 18_000_00, false, "租", 0xFF15CDB1, ruleHint = "按城市规模每月 800/1100/1500 元，与房贷利息不可同时享受"),
    DeductionOption("赡养老人", 36_000_00, true, "养", 0xFF6C7CF0, ruleHint = "独生子女每月 3000 元，非独生子女分摊且每人不超过 1500 元"),
    DeductionOption("3岁以下婴幼儿照护", 24_000_00, true, "婴", 0xFFFF9F43, ruleHint = "每个婴幼儿每月 2000 元，父母可各扣 50%"),
    DeductionOption("个人养老金", 12_000_00, false, "老", 0xFF26B4A8, ruleHint = "按年度缴费额据实扣除，上限 12000 元")
)

/** 表单字段类型，决定键盘与控件形态。 */
enum class DeductionFieldKind { Text, IdCard, Date, Amount, Choice }

/** 一个分项表单字段。 */
data class DeductionField(
    val key: String,
    val label: String,
    val placeholder: String,
    val kind: DeductionFieldKind = DeductionFieldKind.Text,
    val options: List<String> = emptyList()
)

/**
 * 各扣除项目的分项表单。真实 APP 要求逐项填写实体信息（子女、房屋、被赡养人等）
 * 才能提交，这里按同一顺序给出最小必要字段。
 */
val DEDUCTION_FIELDS: Map<String, List<DeductionField>> = mapOf(
    "子女教育" to listOf(
        DeductionField("child_name", "子女姓名", "请输入子女姓名"),
        DeductionField("child_id", "子女证件号码", "请输入 18 位身份证号", DeductionFieldKind.IdCard),
        DeductionField("child_birth", "出生日期", "如 2015-09-01", DeductionFieldKind.Date),
        DeductionField(
            "education_stage", "当前受教育阶段", "请选择",
            DeductionFieldKind.Choice,
            listOf("学前教育", "义务教育", "高中阶段教育", "高等教育")
        )
    ),
    "继续教育" to listOf(
        DeductionField(
            "education_type", "继续教育类型", "请选择",
            DeductionFieldKind.Choice,
            listOf("学历（学位）继续教育", "职业资格继续教育")
        ),
        DeductionField("start_date", "入学或发证日期", "如 2026-03-01", DeductionFieldKind.Date),
        DeductionField("cert_name", "证书名称", "学历教育可填“在读”", DeductionFieldKind.Text),
        DeductionField("cert_no", "证书编号", "职业资格教育必填", DeductionFieldKind.Text)
    ),
    "大病医疗" to listOf(
        DeductionField("patient_name", "患者姓名", "请输入患者姓名"),
        DeductionField(
            "relation", "与纳税人关系", "请选择",
            DeductionFieldKind.Choice,
            listOf("本人", "配偶", "未成年子女")
        ),
        DeductionField("total_amount", "医药费用总金额（元）", "医保系统记录的总金额", DeductionFieldKind.Amount),
        DeductionField("self_paid", "医保目录内个人自付（元）", "用于计算可扣除金额", DeductionFieldKind.Amount)
    ),
    "住房贷款利息" to listOf(
        DeductionField("house_address", "房屋坐落地址", "请输入房屋所在地址"),
        DeductionField("contract_no", "贷款合同编号", "请输入贷款合同编号"),
        DeductionField("bank_name", "贷款银行", "请输入贷款银行"),
        DeductionField("first_repay", "首次还款日期", "如 2021-05-20", DeductionFieldKind.Date)
    ),
    "住房租金" to listOf(
        DeductionField("rent_address", "租赁房屋地址", "请输入租赁房屋所在地址"),
        DeductionField(
            "lessor_type", "出租方类型", "请选择",
            DeductionFieldKind.Choice,
            listOf("自然人", "组织")
        ),
        DeductionField("lessor_name", "出租方姓名或名称", "请输入出租方信息"),
        DeductionField("rent_contract", "租赁合同编号", "请输入租赁合同编号"),
        DeductionField("rent_start", "租赁期起", "如 2026-01-01", DeductionFieldKind.Date),
        DeductionField("rent_end", "租赁期止", "如 2026-12-31", DeductionFieldKind.Date)
    ),
    "赡养老人" to listOf(
        DeductionField("elder_name", "被赡养人姓名", "请输入被赡养人姓名"),
        DeductionField("elder_id", "被赡养人证件号码", "请输入 18 位身份证号", DeductionFieldKind.IdCard),
        DeductionField(
            "elder_relation", "与纳税人关系", "请选择",
            DeductionFieldKind.Choice,
            listOf("父亲", "母亲", "其他法定赡养人")
        ),
        DeductionField(
            "only_child", "是否独生子女", "请选择",
            DeductionFieldKind.Choice,
            listOf("是", "否")
        ),
        DeductionField(
            "share_type", "分摊方式", "请选择",
            DeductionFieldKind.Choice,
            listOf("均摊", "约定分摊", "指定分摊")
        )
    ),
    "3岁以下婴幼儿照护" to listOf(
        DeductionField("baby_name", "子女姓名", "请输入子女姓名"),
        DeductionField("baby_id", "子女证件号码", "请输入 18 位身份证号", DeductionFieldKind.IdCard),
        DeductionField("baby_birth", "出生日期", "如 2024-08-12", DeductionFieldKind.Date)
    ),
    "个人养老金" to listOf(
        DeductionField("voucher_no", "缴费凭证号", "请输入个人养老金缴费凭证号"),
        DeductionField("paid_amount", "本年度缴费金额（元）", "上限 12000 元", DeductionFieldKind.Amount)
    )
)

/** 扣除金额计算规则。金额单位统一为“分”。 */
object DeductionRules {

    /** 大病医疗起扣线：个人自付 15000 元。 */
    const val MEDICAL_THRESHOLD_CENTS = 15_000_00L

    /** 大病医疗年度扣除限额 80000 元。 */
    const val MEDICAL_CAP_CENTS = 80_000_00L

    /** 个人养老金年度缴费上限 12000 元。 */
    const val PENSION_CAP_CENTS = 12_000_00L

    /** 大病医疗：自付超过 15000 元的部分据实扣除，封顶 80000 元。 */
    fun medicalDeductibleCents(selfPaidCents: Long): Long =
        (selfPaidCents - MEDICAL_THRESHOLD_CENTS).coerceIn(0L, MEDICAL_CAP_CENTS)

    /**
     * 按填报内容算出本年可扣除金额：
     * 定额项目 = 年度标准 × 扣除比例；大病医疗与个人养老金按实际金额折算。
     */
    fun amountOf(option: DeductionOption, ratioPercent: Int, details: Map<String, String>): Long =
        when (option.type) {
            "大病医疗" -> {
                val selfPaid = parseYuanToCents(details["self_paid"] ?: "") ?: 0L
                medicalDeductibleCents(selfPaid)
            }
            "个人养老金" -> {
                val paid = parseYuanToCents(details["paid_amount"] ?: "") ?: 0L
                paid.coerceAtMost(PENSION_CAP_CENTS)
            }
            else -> option.yearlyStandardInCents * ratioPercent / 100
        }

    /**
     * 计算实际扣除额：赡养老人非独生子女时，每人分摊不得超过 1500 元/月（18000 元/年）。
     * 其余项目按 [amountOf] 结果返回。
     */
    fun effectiveAmountOf(
        option: DeductionOption,
        ratioPercent: Int,
        details: Map<String, String>
    ): Long {
        val raw = amountOf(option, ratioPercent, details)
        if (option.type == "赡养老人" && details["only_child"] == "否") {
            return raw.coerceAtMost(18_000_00L)
        }
        return raw
    }

    /** 表单必填校验：所有字段都要有值，数字字段必须能解析成正数金额。 */
    fun validate(option: DeductionOption, details: Map<String, String>): String? {
        val fields = DEDUCTION_FIELDS[option.type].orEmpty()
        fields.forEach { field ->
            if (details[field.key].isNullOrBlank()) {
                return "请填写${field.label}"
            }
        }
        fields.filter { it.kind == DeductionFieldKind.Amount }.forEach { field ->
            if (parseYuanToCents(details[field.key].orEmpty()) == null) {
                return "${field.label}需为大于 0 的数字"
            }
        }
        if (option.type == "大病医疗") {
            val total = parseYuanToCents(details["total_amount"].orEmpty())
            val selfPaid = parseYuanToCents(details["self_paid"].orEmpty())
            if (total != null && selfPaid != null && selfPaid > total) {
                return "个人自付金额不能大于医药费用总金额"
            }
        }
        return null
    }
}
