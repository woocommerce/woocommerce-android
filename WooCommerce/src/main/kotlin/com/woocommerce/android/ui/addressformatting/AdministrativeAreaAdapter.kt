package com.woocommerce.android.ui.addressformatting

import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode

object AdministrativeAreaAdapter {

    fun determineAreaForLibAddressInputLibrary(countryCode: LocationCode, adminArea: Location): String =
        when (countryCode) {
            "JP" -> {
                japanPrefectures.first {
                    it.isoCode == adminArea.code
                }.nativeName
            }
            "TR" -> {
                adminArea.name
            }
            else -> adminArea.code
        }

    private val japanPrefectures = listOf(
        AdministrativeArea("Aqichi", "愛知県", "JP23"),
        AdministrativeArea("Akita", "秋田県", "JP05"),
        AdministrativeArea("Aomori", "青森県", "JP02"),
        AdministrativeArea("Chiba", "千葉県", "JP12"),
        AdministrativeArea("Ehime", "愛媛県", "JP38"),
        AdministrativeArea("Fukui", "福井県", "JP18"),
        AdministrativeArea("Fukuoka", "福岡県", "JP40"),
        AdministrativeArea("Fukushima", "福島県", "JP07"),
        AdministrativeArea("Gifu", "岐阜県", "JP21"),
        AdministrativeArea("Gunma", "群馬県", "JP10"),
        AdministrativeArea("Hiroshima", "広島県", "JP34"),
        AdministrativeArea("Hokkaido", "北海道", "JP01"),
        AdministrativeArea("Hyōgo", "兵庫県", "JP28"),
        AdministrativeArea("Ibaraki", "茨城県", "JP08"),
        AdministrativeArea("Ishikawa", "石川県", "JP17"),
        AdministrativeArea("Iwate", "岩手県", "JP03"),
        AdministrativeArea("Kagawa", "香川県", "JP37"),
        AdministrativeArea("Kagoshima", "鹿児島県", "JP46"),
        AdministrativeArea("Kanagawa", "神奈川県", "JP14"),
        AdministrativeArea("Kōchi", "高知県", "JP39"),
        AdministrativeArea("Kumamoto", "熊本県", "JP43"),
        AdministrativeArea("Kyōto", "京都府", "JP26"),
        AdministrativeArea("Mie", "三重県", "JP24"),
        AdministrativeArea("Miyagi", "宮城県", "JP04"),
        AdministrativeArea("Miyazaki", "宮崎県", "JP45"),
        AdministrativeArea("Nagano", "長野県", "JP20"),
        AdministrativeArea("Nagasaki", "長崎県", "JP42"),
        AdministrativeArea("Nara", "奈良県", "JP29"),
        AdministrativeArea("Niigata", "新潟県", "JP15"),
        AdministrativeArea("Ōita", "大分県", "JP44"),
        AdministrativeArea("Okayama", "岡山県", "JP33"),
        AdministrativeArea("Okinawa", "沖縄県", "JP47"),
        AdministrativeArea("Ōsaka", "大阪府", "JP27"),
        AdministrativeArea("Saga", "佐賀県", "JP41"),
        AdministrativeArea("Saitama", "埼玉県", "JP11"),
        AdministrativeArea("Shiga", "滋賀県", "JP25"),
        AdministrativeArea("Shimane", "島根県", "JP32"),
        AdministrativeArea("Shizuoka", "静岡県", "JP22"),
        AdministrativeArea("Tochigi", "栃木県", "JP09"),
        AdministrativeArea("Tokushima", "徳島県", "JP36"),
        AdministrativeArea("Tōkyō", "東京都", "JP13"),
        AdministrativeArea("Tottori", "鳥取県", "JP31"),
        AdministrativeArea("Toyama", "富山県", "JP16"),
        AdministrativeArea("Wakayama", "和歌山県", "JP30"),
        AdministrativeArea("Yamagata", "山形県", "JP06"),
        AdministrativeArea("Yamaguchi", "山口県", "JP35"),
        AdministrativeArea("Yamanashi", "山梨県", "JP19"),
    )
}
