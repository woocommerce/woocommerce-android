package com.woocommerce.android.push

import com.woocommerce.android.model.Notification

object NotificationTestUtils {
    const val TEST_ORDER_NOTE_FULL_DATA_1 = "eNqlVFtr2zAU/iuaGGOjdiLbSZwYyqDdwyhbW2gZjKkUxVZibbYkdKkbSv77jpz" +
        "L3O5hlL3Yks79fN85T1gqxy0ufjxhUeFiOskzkiQzkkbYbTTHBbZOGX6vTMUNjrCXhjNQlL5pImyl0Jq7w3Wpqk3vyvFHeMSg3/JKsN3bzp" +
        "1o2ZqDQMhKlH1kEpG7CNdcrOtgNJmDtBOVqw8Xbxo41s5pW9AxHdtRp0elaum403GppOPS0XHrY934tZCWjvui6LiPBf++hLhkxsUC9Edar" +
        "vH2bhsdE70K1aFL3y65KVCySKZUfmKOF+jCS46SPEIpSRMqb5VjTYHezsl8tFgQQuU127QQH33lrlZVgc4NVOzQOTMVen/jjND8A5U3NTR" +
        "KyPVR7fjQ9g8owYN0ro2qfOmgWgkBjHjoUzk5Qxes5RZ9UV6uOfWEMHJeM2FQOFc5SgbaZ5xJwVEnXL3T/KLW6qCYUvlZqWovR0NR8KEa9b" +
        "cgo3KYI/Vpki2oX3GyQt8E79DVniKGQXY7Sr1EjnUqIONsrfSo3VjHAK91x5dWOH6ElFWtkHSslXUjXeuP4XAaQHnHSieUPA0dfkGhJAMOB" +
        "QbjKUg0M4DJgZZ75jVC/gqw3wVWOiBloLwNvxAdF8ksm6ZZPplFeMf2IsTcRr3hQG9Qj/bLRpQx02LUgY023NpdGXACTj4kQD7Rc/HoHR/d" +
        "v9JRbxU8QVZ4C3lB+5y3x1n0y5+8dMP5+648qtkDRwxJAKh38AbwrOZZCd/VfDFE9PKgglbK/KE4UhL9E7fnsB82R2jXEKY0j2Zkj9Og28/" +
        "RejVn9pg6AbPhWKvBOgxrTGZxkt+SWTFNi2x6QkhBCA56ruH7cg+UDVvh/1aM1xWMXax32yBOH/slE4X1uncOozKZ5Dik2jDr7i3n8j4kDb" +
        "JklmaLJM2ngbyyX0PQoe1vYNrb/g=="
    const val TEST_ORDER_NOTE_FULL_DATA_2 = "eNqlVGtr2zAU/SuaGGNjdiI7b0MZo/u4tYWWjTGXotiqrc2WhHRVN5T89105ie" +
        "f2W5kxtizd5znn+okqDcLR7NcTlSXNFvPVjCXJkqURhZ0RNKMOtBV32pbC0oh6ZQVHQ+WbJqJOSWMEnD63utz1oUA84iZF+1aUkh/2DuFk" +
        "yyuBB1KVsugzs4jdRrQWsqqD03yNp50soT59eNvgsgYwLsun+dRNOjMpdJtPOxMXWoFQkE9bH5vGV1K5fNo3lU/7XPjuW4gLbiGWaD8xqq" +
        "L72300FHoZuiMXvt0Km5Fks0py9YWDyMhnX3kHZBGRlKW4e6OBNxl5O2cThleurviuxfzkm4Balxk5t9gxkHNuS/L+Gqw04kOurmsESqpq" +
        "MBs22n6DpHRUzpXVpS8Au1WYQDea5J6xckVCATG6WiCdhJp81dX4bBwj92ky2+T+XrB78l2KjlweKbRcVUfKXyLLOx2QA1drM2l3DjjiWX" +
        "Vi6ySIAXJetlLlU6MdTExtPoXFWQDtHS9AanUWEHhBcTJDjoPCaGDUcIuYnWRzVEYj1Z9Ay21QDaBogiRdeIXsNEuWs0U6W82XET2oMQs5" +
        "91HvOLIb9WP8tpFFzI2cdOhjrHDu0AauUDMPCYpD9loZotMh/CsD9V4hElZF91gXwgfeDbPit79FAeP5+Kk9qfmDIJwoJKgP8Ab5LNezAp" +
        "/3682Y0YuTCbnXdlAg0Yr80JrcYB2kB+AZw6chPhz8YyRdRrPNkZIRsM+JebU8jvSBbNGKtwa9w9jEbB2zxQ3bZGyB90fGMsZosINGHDs7" +
        "qTMM6P9Nuzcljm5sDoMZp4/9vEfhT3cMjlMxn69oKLXhDu6cEOouFI1nyTJdJ7PVch0KVP0fARHa/wW+R7Nm"
    const val TEST_ORDER_NOTE_FULL_DATA_SITE_2 = "eNqVU1tr2zAU/iuaGHuZL7LjJI6hlNE9bm0HZTDmUhRLidXZktClWSj57ztynNQt" +
        "e+mLrcu56LucZyyV4xZXv5+xYLiaL4plVi7KfB5ht9ccV9g6ZfiDMowbHGEvDacQKH3XRdhKoTV3p+1asf1QyvG/cIghvudM0OPZsZzo6" +
        "ZbDhZBMNENnEpH7CLdcbNuQVJRwuxPMtaeNNx0sW+e0req0Tm2y00mj+jrd6bhR0nHp6rT3se78VkhbpwOoOh16wX+AEDfUuFhAfKLlFh" +
        "/uD9H5oTcBHbr2/ZqbCmV5Vsuv1PEKffFbbx3KswjlJBzfKUe7Cn3MSUJILW/pvofu6Dt3rWIVujKA1yFoxVCdIsbX4w5Put0axXzjAIy" +
        "ECob3wvfoh6edcHtUe0LYEkGraUrt82y2qv2Gkw36KfgO3YyCGCq3o4BveQKKdCcaAMLiTnEjePLoDbVWNIkU8pEOBFLWC1mnWlmX6FZf" +
        "hsUFUPCJNk4oeREAvdErm4FgwS44hxtNDVBw8sAocyfkn8DxfbCAAwcEf9nws8JBQLZaLOerZbmM8NFaFbQ8REPeJGyCRvs1gImpFskOU" +
        "rTh1h5dACvQ/ykDocWg+7k4Pld/Z6EhK1TKM3yAZ1lHnbdn2/v1I2/c1Oq/lEctfeKIIgnqDPkfQExWzhr4bsrVVM7rUwjaKDOaCSmJrl" +
        "4UQ98GxV4rfBrJwM1UkryIinzUZMLsa2XebY9RPyd64IX2OugNQxCTMs6zOzKrikWVF58JqQjBIc51fAR3cmcYt1dN/2PA8wR73SnKgPW" +
        "cZGWdkvlL+BAdO6DCHkGFGb7cXawW0ASmfewDA1IUSxxe3VHrHizn8iG8H+6yRb6aZ8uyDJ6Vw6gDWYd/EqSmUg=="
    const val TEST_REVIEW_NOTE_FULL_DATA_1 = "eNrdVtuK2zAQ/ZVUZfelTiz5lsQQli1l97WUvtUhyLaSqLVlIclJw5J/78h2nNu27JZ9" +
        "aSEQeaSZM3PmjOwnJCrDNIq/PSGeoziMcDCd+pPId5DZSYZilFVlyYRBDqqFYhQOibooHKQFl5KZw2Na5bsmjGE/wYjuBTeUgpeiYtUhdB" +
        "FrzRRscJHzrNnBTjTfzx1UspzT05O8pCt2cRTDwTXjq7VF8cIItrc8N+v+qVYFrNfGSB0nbuKS0UrRDTVUjaCWxG3XiTsOApbiHE8mXhYE" +
        "2GeeF1D4p2w59TBZ3ukZBLzNZ02oG//+xnuAn2ZZrdhZTLC2D3aRhyQKIQohWR5mNPBDmmXTNI1Czw/96MZ/gFifbGQ1e0Rt2QaqfkIFFz" +
        "+gxqf93jmjau/0rD5WVf7uWVLlBU0hBLYdRR6BHUmV7WHTKwtJM8MrYcGQlrQcHpocL2mhGcArqtdXViqlqjbsyg5tM0ejUTXYFJPFzlTn" +
        "5v2xWJ436P12EJAJ5FlpWPsRyIsbKItEPtA2DiLwPNBz9DlpsqzTgmdDKvloW6lcKqZ1225YmcTdkMTt/HTiWjB0QHtlFOsEIXwrtTbJVw" +
        "awThCgLw2d9Ps4bALOLwq2YSBm3FK86HYXlokzWLqt7Bgbva7kqNxpQ1dcrLYstVgt/lYOaV5y0dMwkmt51+pgZqN35tts1rBzIrovbMPZ" +
        "drCs1OAjo4KzRCS1F+Fw8Pu/aHCu0suhfEHGoLW8zoC0tAV1zxQOavEmvcbHFxrvCW2osrW8fQLEIeM+geDPCcxP57yTfjPbtrd/MwOt8y" +
        "uVZ51AeRg5/+UMQQ9Nrfv3U51+Z5m5ficNCrY0AzpQrawr0an65e+qrukkurxZneN1bGk59fMCx8edp+3tuVjeSJ12d2ERjpqZX749EvFs" +
        "oceb57laO2W+ec7vO9hhp6brAlqFLVra2jkyvAQYWko7eNgjQzwZ4vArjmJCYm/8AeMYW40bbgpb2ucWddDeY7bCrBL/zieCY7/RupSTeu" +
        "lDbZaFggItmjGxsHw0cvQmxB9HE1u7qMvU3hBk/wviuD2r"
    const val TEST_REVIEW_NOTE_FULL_DATA_2 = "eNrdVk2P2jAQ/SvI1e6lgSQONhAJbVeqtteq7a1ByEkMeJs4lu1AEeK/d5yE71XVrfbSS" +
        "hzssWee582bCTskK8sNir/vkMhRTGhAIkomEfWQ3SqOYpRVZcmlRR6qpeYMLsm6KDxkpFCK28M2rfJtE8byn2BEX2vNnsFJM7nsALqAteE" +
        "aDoTMRdacBB6Z7WceKnku2PlNUbIlv7oawMUVF8uVA8GEwvFG5HZ13NW6gPXKWmXixE/8YLDUbM0s0wNIJfHbdeJPcEiHOCA0ZKN0lI4xz" +
        "QjnhAxxxDDBkwczhYD3+bQJdRc93uEn+Bme1ZpfxARru3GLnISUBBELwywnGRtGhGXZJE0pwRFQexc9QayPLrKefkJt2hay3qFCyB+Q426" +
        "/9y6o2nsnUjec2w8vsqqueKIQ2VUUhWM4UUy7Gja1cpgss6KSDg0Zxcr+ocjxghWGA75mZnVjZUrpas1v7FA3ezJaXYNNc1VsbXVp3p+yF" +
        "XmDfjweDqMRvLMysKYY5CUspBXSCHgbDSl4Hvg5+ZxVWdVpIbI+U2KwqXSuNDemrTesbOKvw8Tv/EziOzB0QHtlFOcEIShGh0e+MoBzggD" +
        "H1NBZwU/NJuH+vOBrDmoOWorn3encMXEByzaVa2NrVpUalFtj2VLI5YanDqvF36g+y0shjzQM1Eo9tDqYuuid+T6bNuycqe4LXwu+6S0q3" +
        "Xss0rq81N91v/3BW0BFeZ0BHcyFS/wL6YahFx7Fi8Mr8R6ZajjYz84bqJNU0zSOs7/RVuv8yoo6J6hogLz/UptQQlub49yv02ee2ZtZ3yv" +
        "4wvZYT7dqqeRLYvndJ+Awr6LreeWdhpwj5dwPRx4ed56uspdSeRtpusO5AzgJZnYzkxP5YqKnfj7PddS9uNPlW7/5XYfa76R0m0Arr3nLW" +
        "ttEVpSAwkrlui7AYT8Y9wPyLZjE0SgOhu+DIA6cwK2whcvscwvaa4eDSzCr5L/z4fXcH5/uyUm9iCA3x0LBgBbDuZw7PpwaKR6H0YiOXe6" +
        "yLlM3HsL9LxNBBjo="
    const val TEST_REVIEW_NOTE_FULL_DATA_SITE_2 = "eNrNVm2L4zYQ/ivGZe9L7ViybMcOhOOgXOmHQmlLodQhyJa80daWhSxnG5b89478" +
        "ksS53nHLXUshBHkkPZp59MyMXlzZGt65mz9eXMHcTZxEKVpHKMWea06Kuxu3bJuGS+N6bi81p7BI9nXtuZ0USnEzfxYtOw0whv8FRvedFI" +
        "ZS58dem8MJNmsqH28OwiRFWZbG68s5fcc1rBOSiXJYiDxMdued5zacCTpij0tFQx/53VoECw9cPB7s4WGcwPSzYOZw+ep1DeODMarb5EEe" +
        "4NWjpkdqqF5BiHkwjvMgCrMkLbKIAxFxTOKsKqoyzpK4YCGnCX7bbQHwDdsOUA/k3UP4Hn4dL3vNF5hgHT/sgMU4iRGhGJcsLmlEYlqWWV" +
        "EkcUhikjyQ94D1nUXW2+/dMWwDUb+4tZB/QowvZwiY2cFI1ZXC83lJ4tm73MJvVAta1NxRumV9aRzRORI4c/KepYTBP0fsdry8qQlV3XEd" +
        "ZjtvuEUXpzClqLYCGYRgHaelEa0cPO0UbfxZQZuK1h0HXzXtDh9YqQIfj/xqN7oHM9y9ubdpruqTaZfm85WxiabLdJh695yB120HUxECJQ" +
        "sDUeIsWcfZOl0D0Ez5FeJGOKovalH6VInVc6uZ0rzrRgnByOTBEefBtK/Lg9AyNB7+Sgy7CQAuLruzz6/EsZsAJ0LuHOorAewm68hMkHsj" +
        "uGt1kLB+X/MjhzRD473tp9m95XNxLEwoOJcazvy65Vrw1VMPsuhEuZJCPtE8eFY+ZY2QFzJX6qDejtraWvTJ/KbcAsc3mv+ZHwV/dqpWO7" +
        "8chHJ+b3vt/MA4XWr7vh58hktTEgEjgOufANcXgOuTPFhkCMZeGM8pEuK7FLlwN7By3t3m+iLBgcWFhD9LsV8iNauQr6T4/5FSO0NN313a" +
        "Vl888dJ8tFU5Na+MQx09qqiVnxTRJ3vXXCPJfY30rpXV0nS7kWAvmqVjb3opnK+uWUvYfujHFy3tvqR95PIf+blWiVuKCJoCHcT9Lwb6zX" +
        "S+P0jzw5hHue5HzseENKIBVdFG2QxGIfZR6of4V0Q2JIYu8i1CG2RlaoSpbYQ/TQSNxccGWrZyEQL6yHsjQYRkSRjyijN4DZB1saYERzhG" +
        "CKdVWf0n7w3PvgMnl/O+IhCbZaGmQEvHudxbPqyWkzCLkiwlthvJvimGpnr+G+PBX8Q="

    fun generateTestNewOrderNotificationPayload(
        userId: Long = 12345,
        noteType: String = "new_order",
        noteId: String = "5456765434",
        noteData: String = TEST_ORDER_NOTE_FULL_DATA_1
    ) = mapOf(
        "type" to noteType,
        "user" to userId.toString(),
        "note_id" to noteId,
        "note_full_data" to noteData
    )

    fun generateTestNewReviewNotificationPayload(
        userId: Long = 12345,
        noteType: String = "product_review",
        noteId: String = "5604993863",
        noteData: String = TEST_REVIEW_NOTE_FULL_DATA_1
    ) = mapOf(
        "type" to noteType,
        "user" to userId.toString(),
        "note_id" to noteId,
        "note_full_data" to noteData
    )

    fun generateTestNotification(
        noteId: Int = 0,
        remoteNoteId: Long,
        remoteSiteId: Long,
        uniqueId: Long,
        channelType: NotificationChannelType,
        noteType: WooNotificationType
    ) = Notification(
        noteId = noteId,
        uniqueId = uniqueId,
        remoteNoteId = remoteNoteId,
        remoteSiteId = remoteSiteId,
        channelType = channelType,
        noteType = noteType,
        noteTitle = "",
        noteMessage = "",
        icon = ""
    )
}
