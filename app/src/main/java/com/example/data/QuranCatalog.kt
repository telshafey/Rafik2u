package com.example.data

import android.icu.util.IslamicCalendar
import com.example.model.MutashabihItem
import com.example.model.QuranJuzInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QuranCatalog {

    private val hijriMonthsAr = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun todayDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun formatHijriDateAr(date: Date = Date()): String {
        val cal = IslamicCalendar()
        cal.time = date
        val day = cal.get(IslamicCalendar.DAY_OF_MONTH)
        val monthIdx = cal.get(IslamicCalendar.MONTH).coerceIn(0, 11)
        val year = cal.get(IslamicCalendar.YEAR)
        return "$day ${hijriMonthsAr[monthIdx]} $year هـ"
    }

    fun formatGregorianDateAr(date: Date = Date()): String =
        SimpleDateFormat("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar")).format(date)

    val allJuz: List<QuranJuzInfo> = listOf(
        QuranJuzInfo(1, "الجزء الأول (آلم)", "الفاتحة — البقرة ١٤١"),
        QuranJuzInfo(2, "الجزء الثاني (سيقول)", "البقرة ١٤٢ — ٢٥٢"),
        QuranJuzInfo(3, "الجزء الثالث (تلك الرسل)", "البقرة ٢٥٣ — آل عمران ٩٢"),
        QuranJuzInfo(4, "الجزء الرابع (لن تنالوا)", "آل عمران ٩٣ — النساء ٢٣"),
        QuranJuzInfo(5, "الجزء الخامس (والمحصنات)", "النساء ٢٤ — ١٤٧"),
        QuranJuzInfo(6, "الجزء السادس (لا يحب الله)", "النساء ١٤٨ — المائدة ٨١"),
        QuranJuzInfo(7, "الجزء السابع (وإذا سمعوا)", "المائدة ٨٢ — الأنعام ١١٠"),
        QuranJuzInfo(8, "الجزء الثامن (ولو أننا)", "الأنعام ١١١ — الأعراف ٨٧"),
        QuranJuzInfo(9, "الجزء التاسع (قال الملأ)", "الأعراف ٨٨ — الأنفال ٤٠"),
        QuranJuzInfo(10, "الجزء العاشر (واعلموا)", "الأنفال ٤١ — التوبة ٩٢"),
        QuranJuzInfo(11, "الجزء الحادي عشر (يعتذرون)", "التوبة ٩٣ — هود ٥"),
        QuranJuzInfo(12, "الجزء الثاني عشر (وما من دابة)", "هود ٦ — يوسف ٥٢"),
        QuranJuzInfo(13, "الجزء الثالث عشر (وما أبرئ)", "يوسف ٥٣ — الحجر ١"),
        QuranJuzInfo(14, "الجزء الرابع عشر (ربما)", "الحجر ٢ — النحل ١٢٨"),
        QuranJuzInfo(15, "الجزء الخامس عشر (سبحان الذي)", "الإسراء ١ — الكهف ٧٤"),
        QuranJuzInfo(16, "الجزء السادس عشر (قال ألم)", "الكهف ٧٥ — طه ١٣٥"),
        QuranJuzInfo(17, "الجزء السابع عشر (اقترب)", "الأنبياء ١ — الحج ٧٨"),
        QuranJuzInfo(18, "الجزء الثامن عشر (قد أفلح)", "المؤمنون ١ — الفرقان ٢٠"),
        QuranJuzInfo(19, "الجزء التاسع عشر (وقال الذين)", "الفرقان ٢١ — النمل ٥٥"),
        QuranJuzInfo(20, "الجزء العشرون (أمن خلق)", "النمل ٥٦ — العنكبوت ٤٥"),
        QuranJuzInfo(21, "الجزء الحادي والعشرون (اتل ما أوحي)", "العنكبوت ٤٦ — الأحزاب ٣٠"),
        QuranJuzInfo(22, "الجزء الثاني والعشرون (ومن يقنت)", "الأحزاب ٣١ — يس ٢٧"),
        QuranJuzInfo(23, "الجزء الثالث والعشرون (وما لي)", "يس ٢٨ — الزمر ٣١"),
        QuranJuzInfo(24, "الجزء الرابع والعشرون (فمن أظلم)", "الزمر ٣٢ — فصلت ٤٦"),
        QuranJuzInfo(25, "الجزء الخامس والعشرون (إليه يرد)", "فصلت ٤٧ — الجاثية ٣٧"),
        QuranJuzInfo(26, "الجزء السادس والعشرون (حم)", "الأحقاف ١ — الذاريات ٣٠"),
        QuranJuzInfo(27, "الجزء السابع والعشرون (قال فما خطبكم)", "الذاريات ٣١ — الحديد ٢٩"),
        QuranJuzInfo(28, "الجزء الثامن والعشرون (قد سمع)", "المجادلة ١ — التحريم ١٢"),
        QuranJuzInfo(29, "الجزء التاسع والعشرون (تبارك)", "الملك ١ — المرسلات ٥٠"),
        QuranJuzInfo(30, "الجزء الثلاثون (عمّ)", "النبأ ١ — الناس ٦")
    )

    val mutashabihatList: List<MutashabihItem> = listOf(
        MutashabihItem(
            id = "mut_1",
            title = "المفلحون والمهتدون في سورة البقرة",
            firstPlace = "البقرة (٥): «وَأُولَٰئِكَ هُمُ الْمُفْلِحُونَ»",
            secondPlace = "البقرة (١٥٧): «وَأُولَٰئِكَ هُمُ الْمُهْتَدُونَ»",
            memoryTip = "في أول البقرة الفلاح مقرون بالهدى والإيمان بالغيب، وفي آية الصابرين الختام بالمهتدين بعد ذكر الصلوات والرحمة.",
            juzRangeAr = "ج١ — ج٢"
        ),
        MutashabihItem(
            id = "mut_2",
            title = "رب اجعل هذا بلداً آمناً",
            firstPlace = "البقرة (١٢٦): «رَبِّ اجْعَلْ هَٰذَا بَلَدًا آمِنًا» (نكرة)",
            secondPlace = "إبراهيم (٣٥): «رَبِّ اجْعَلْ هَٰذَا الْبَلَدَ آمِنًا» (معرّف بـ أل)",
            memoryTip = "دعاء البقرة سبق البناء وتعمير البلد فجاء نكرة، ودعاء إبراهيم بعد استقرار الذرية والسكن فجاء معرفاً بـ(البلد).",
            juzRangeAr = "ج١ — ج١٣"
        ),
        MutashabihItem(
            id = "mut_3",
            title = "ولكم فيها منافع أم أكل ونسقيكم",
            firstPlace = "البقرة (٢١٩): «وَإِثْمُهُمَا أَكْبَرُ مِن نَّفْعِهِمَا»",
            secondPlace = "النحل (٦٦) والمؤمنون (٢١): «نُّسْقِيكُم مِّمَّا فِي بُطُونِهِ» (تذكير) و«بُطُونِهَا» (تأنيث)",
            memoryTip = "في النحل الضمير مذكر (بطونه) عائداً على لفظ الجمع باعتبار النوع، وفي المؤمنون مؤنث (بطونها) متطابقاً مع الأنعام.",
            juzRangeAr = "ج١٤ — ج١٨"
        ),
        MutashabihItem(
            id = "mut_4",
            title = "سارعوا وسابقوا إلى المغفرة",
            firstPlace = "آل عمران (١٣٣): «وَسَارِعُوا إِلَىٰ مَغْفِرَةٍ مِّن رَّبِّكُمْ»",
            secondPlace = "الحديد (٢١): «سَابِقُوا إِلَىٰ مَغْفِرَةٍ مِّن رَّبِّكُمْ»",
            memoryTip = "آل عمران بالواو والسين والراء (وسارعوا)، والحديد بدون واو وبلفظ السبق (سابقوا) مع وصف عرض السماء والأرض.",
            juzRangeAr = "ج٤ — ج٢٧"
        ),
        MutashabihItem(
            id = "mut_5",
            title = "ختام صفات الحذر والتكذيب في البقرة وآل عمران",
            firstPlace = "آل عمران (١٧٦): «وَلَهُمْ عَذَابٌ عَظِيمٌ»",
            secondPlace = "آل عمران (١٧٧): «وَلَهُمْ عَذَابٌ أَلِيمٌ» و(١٧٨): «وَلَهُمْ عَذَابٌ مُّهِينٌ»",
            memoryTip = "رتبها بثلاث كلمات في آل عمران: عظيم (حزن المسارعة للكفر) ثم أليم (اشتراء الكفر) ثم مهين (الإملاء للظالمين).",
            juzRangeAr = "ج٤"
        ),
        MutashabihItem(
            id = "mut_6",
            title = "من إملاق وخشية إملاق",
            firstPlace = "الأنعام (١٥١): «مِّنْ إِمْلَاقٍ ۖ نَّحْنُ نَرْزُقُكُمْ وَإِيَّاهُمْ»",
            secondPlace = "الإسراء (٣١): «خَشْيَةَ إِمْلَاقٍ ۖ نَّحْنُ نَرْزُقُهُمْ وَإِيَّاكُمْ»",
            memoryTip = "في الأنعام الفقر واقع (من إملاق) فبدأ بالآباء (نرزقكم)، وفي الإسراء الفقر متوقع (خشية إملاق) فبدأ بالأبناء (نرزقهم).",
            juzRangeAr = "ج٨ — ج١٥"
        ),
        MutashabihItem(
            id = "mut_7",
            title = "فاصبر إن وعد الله حق",
            firstPlace = "الروم (٦٠): «فَاصْبِرْ إِنَّ وَعْدَ اللَّهِ حَقٌّ ۖ وَلَا يَسْتَخِفَّنَّكَ»",
            secondPlace = "غافر (٥٥ و٧٧): «فَاصْبِرْ إِنَّ وَعْدَ اللَّهِ حَقٌّ وَاسْتَغْفِرْ لِذَنبِكَ»",
            memoryTip = "في ختام الروم تحذير من الاستخفاف، وفي غافر اقتران الصبر بالاستغفار والتسبيح.",
            juzRangeAr = "ج٢١ — ج٢٤"
        ),
        MutashabihItem(
            id = "mut_8",
            title = "فكيف كان عقاب وفكيف كان نكير",
            firstPlace = "الرعد (٣٢) وغافر (٥) وص (١٤): «فَكَيْفَ كَانَ عِقَابِ»",
            secondPlace = "الحج (٤٤) وسبأ (٤٥) وفاطر (٢٦) والملك (١٨): «فَكَيْفَ كَانَ نَكِيرِ»",
            memoryTip = "تأتي (عقاب) في مواضع ثلاثة (الرعد وص وغافر)، بينما يغلب لفظ (نكير) في الحج وسبأ وفاطر والملك.",
            juzRangeAr = "ج١٣ — ج٢٩"
        ),
        MutashabihItem(
            id = "mut_9",
            title = "ألا إن وعد الله حق ولكن أكثرهم",
            firstPlace = "يونس (٥٥): «أَلَا إِنَّ وَعْدَ اللَّهِ حَقٌّ وَلَٰكِنَّ أَكْثَرَهُمْ لَا يَعْلَمُونَ»",
            secondPlace = "لقمان (٣٣): «إِنَّ وَعْدَ اللَّهِ حَقٌّ ۖ فَلَا تَغُرَّنَّكُمُ الْحَيَاةُ الدُّنْيَا»",
            memoryTip = "في يونس ختامها بالعلم (لا يعلمون)، وفي لقمان وفاطر النهي عن الاغترار بالدنيا.",
            juzRangeAr = "ج١١ — ج٢١"
        ),
        MutashabihItem(
            id = "mut_10",
            title = "إن في ذلك لآية وآيات",
            firstPlace = "الشعراء: تتكرر بصيغة الإفراد «إِنَّ فِي ذَٰلِكَ لَآيَةً» بعد كل قصة.",
            secondPlace = "النحل والعنكبوت والروم: تغلب صيغة الجمع «لَآيَاتٍ» مع تنوع الدلائل الكونية.",
            memoryTip = "اربط قصص الأنبياء المتسلسلة في الشعراء بالمفرد (لآية) لتوحيد المشهد القصصي.",
            juzRangeAr = "ج١٤ — ج٢١"
        ),
        MutashabihItem(
            id = "mut_11",
            title = "وما أوتيتم من شيء أم من علم",
            firstPlace = "القصص (٦٠) والشورى (٣٦): «وَمَا أُوتِيتُم مِّن شَيْءٍ فَمَتَاعُ الْحَيَاةِ الدُّنْيَا»",
            secondPlace = "الإسراء (٨٥): «وَمَا أُوتِيتُم مِّنَ الْعِلْمِ إِلَّا قَلِيلًا»",
            memoryTip = "في الإسراء السؤال عن الروح فجاء قيد (من العلم)، وفي القصص والشورى ذكر الرزق والزينة فجاء (من شيء).",
            juzRangeAr = "ج١٥ — ج٢٥"
        ),
        MutashabihItem(
            id = "mut_12",
            title = "ذوقوا عذاب الخلد وعذاب الحريق",
            firstPlace = "السجدة (١٤): «فَذُوقُوا بِمَا نَسِيتُمْ لِقَاءَ يَوْمِكُمْ هَٰذَا... ذُوقُوا عَذَابَ الْخُلْدِ»",
            secondPlace = "آل عمران (١٨١) والأنفال (٥٠) والحج (٢٢): «ذُوقُوا عَذَابَ الْحَرِيقِ»",
            memoryTip = "تنفرد سورة السجدة بلفظ (عذاب الخلد) مع ذكر النسيان، وتجتمع الثلاث الأخرى على (عذاب الحريق).",
            juzRangeAr = "ج٤ — ج٢١"
        ),
        MutashabihItem(
            id = "mut_13",
            title = "الذي خلق السماوات والأرض في ستة أيام",
            firstPlace = "الأعراف (٥٤) ويونس (٣) وهود (٧): تتابع ذكر العرش مع تفصيل الخلق.",
            secondPlace = "الفرقان (٥٩) والسجدة (٤) وق (٣٨) والحديد (٤): تأمل «ثُمَّ اسْتَوَىٰ عَلَى الْعَرْشِ» vs «وَمَا مَسَّنَا مِن لُّغُوبٍ».",
            memoryTip = "في سورة ق بدون استواء على العرش بل خُتمت بـ(وما مسنا من لغوب) رداً على من زعم التعب.",
            juzRangeAr = "ج٨ — ج٢٧"
        ),
        MutashabihItem(
            id = "mut_14",
            title = "موسى وطلبه في الأعراف وطه والقصص",
            firstPlace = "الأعراف (١٤٣): «قَالَ رَبِّ أَرِنِي أَنظُرْ إِلَيْكَ»",
            secondPlace = "طه (٢٥): «قَالَ رَبِّ اشْرَحْ لِي صَدْرِي» • القصص (٣٣): «قَالَ رَبِّ إِنِّي قَتَلْتُ مِنْهُمْ نَفْسًا»",
            memoryTip = "اربط كل موضع بمناسبته: في الأعراف عند الميقات، في طه عند التكليف بالرسالة، في القصص قبل العودة لفرعون.",
            juzRangeAr = "ج٩ — ج٢٠"
        ),
        MutashabihItem(
            id = "mut_15",
            title = "ولقد يسرنا القرآن للذكر في سورة القمر",
            firstPlace = "القمر (١٧ و٢٢ و٣٢ و٤٠): تتكرر الآية أربع مرات عقب قصص الأمم.",
            secondPlace = "تسبقها في قصة لوط (٣٦): «فَتَمَارَوْا بِالنُّذُرِ» وفي عاد (٢١): «فَكَيْفَ كَانَ عَذَابِي وَنُذُرِ».",
            memoryTip = "انتبه لفواصل قصة نوح ولوط وثمود في سورة القمر بضبط موضع (ولقد يسرنا) بعد خاتمة كل قوم.",
            juzRangeAr = "ج٢٧"
        ),
        MutashabihItem(
            id = "mut_16",
            title = "فبأي آلاء ربكما تكذبان vs ويل يومئذ للمكذبين",
            firstPlace = "سورة الرحمن: تتكرر «فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ» خطاباً للثقلين.",
            secondPlace = "سورة المرسلات: تتكرر «وَيْلٌ يَوْمَئِذٍ لِّلْمُكَذِّبِينَ».",
            memoryTip = "الرحمن محورها تعديد النعم بالاستفهام التقريري، والمرسلات محورها الإنذار والتهديد بالويل.",
            juzRangeAr = "ج٢٧ — ج٢٩"
        ),
        MutashabihItem(
            id = "mut_17",
            title = "جنات عدن ومفتحة لهم الأبواب vs تدخلونها",
            firstPlace = "الرعد (٢٣) والنحل (٣١) وفاطر (٣٣): «جَنَّاتُ عَدْنٍ يَدْخُلُونَهَا»",
            secondPlace = "ص (٥٠): «جَنَّاتِ عَدْنٍ مُّفَتَّحَةً لَّهُمُ الْأَبْوَابُ»",
            memoryTip = "في ص بالنصب (جناتِ) وباستقبال الأبواب المفتحة للمتقين.",
            juzRangeAr = "ج١٣ — ج٢٣"
        ),
        MutashabihItem(
            id = "mut_18",
            title = "فلا أقسم في أواخر الحاقة والمعارج والتكوير والانشقاق",
            firstPlace = "الحاقة (٣٨) والمعارج (٤٠): «فَلَا أُقْسِمُ بِمَا تُبْصِرُونَ» و«بِرَبِّ الْمَشَارِقِ وَالْمَغَارِبِ»",
            secondPlace = "التكوير (١٥) والانشقاق (١٦): «فَلَا أُقْسِمُ بِالْخُنَّسِ» و«بِالشَّفَقِ»",
            memoryTip = "جميعها تبدأ بنفي توهم الحاجة للقسم (فلا أقسم)، مع تفاوت المقسم به من كواكب وآفاق في جزء تبارك وعمّ.",
            juzRangeAr = "ج٢٩ — ج٣٠"
        ),
        MutashabihItem(
            id = "mut_19",
            title = "سورة الكافرون وتكرار لا أعبد وما عبدتم",
            firstPlace = "الآية (٢-٣): «لَا أَعْبُدُ مَا تَعْبُدُونَ ۝ وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ»",
            secondPlace = "الآية (٤-٥): «وَلَا أَنَا عَابِدٌ مَّا عَبَدتُّمْ ۝ وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ»",
            memoryTip = "الآية (٢) بالفعل المضارع (لا أعبد) و(تعبدون)، ثم في الآية (٤) بالاسم والماضي (عابدٌ ما عبدتُّم).",
            juzRangeAr = "ج٣٠"
        ),
        MutashabihItem(
            id = "mut_20",
            title = "أواخر النبأ والنازعات وعبس في أهوال الساعة",
            firstPlace = "النبأ (٤٠): «وَيَقُولُ الْكَافِرُ يَا لَيْتَنِي كُنتُ تُرَابًا»",
            secondPlace = "النازعات (٣٤): «فَإِذَا جَاءَتِ الطَّامَّةُ الْكُبْرَىٰ» • عبس (٣٣): «فَإِذَا جَاءَتِ الصَّاخَّةُ»",
            memoryTip = "النازعات فيها (الطامة الكبرى) لعظم مشهد البعث، وعبس فيها (الصاخة) لشدة صيحة الذهول.",
            juzRangeAr = "ج٣٠"
        )
    )

    val allSurahs: List<com.example.model.SurahInfo> = listOf(
        com.example.model.SurahInfo(1, "الفاتحة", 7, listOf(1)),
        com.example.model.SurahInfo(2, "البقرة", 286, listOf(1, 2, 3)),
        com.example.model.SurahInfo(3, "آل عمران", 200, listOf(3, 4)),
        com.example.model.SurahInfo(4, "النساء", 176, listOf(4, 5, 6)),
        com.example.model.SurahInfo(5, "المائدة", 120, listOf(6, 7)),
        com.example.model.SurahInfo(6, "الأنعام", 165, listOf(7, 8)),
        com.example.model.SurahInfo(7, "الأعراف", 206, listOf(8, 9)),
        com.example.model.SurahInfo(8, "الأنفال", 75, listOf(9, 10)),
        com.example.model.SurahInfo(9, "التوبة", 129, listOf(10, 11)),
        com.example.model.SurahInfo(10, "يونس", 109, listOf(11)),
        com.example.model.SurahInfo(11, "هود", 123, listOf(11, 12)),
        com.example.model.SurahInfo(12, "يوسف", 111, listOf(12, 13)),
        com.example.model.SurahInfo(13, "الرعد", 43, listOf(13)),
        com.example.model.SurahInfo(14, "إبراهيم", 52, listOf(13)),
        com.example.model.SurahInfo(15, "الحجر", 99, listOf(13, 14)),
        com.example.model.SurahInfo(16, "النحل", 128, listOf(14)),
        com.example.model.SurahInfo(17, "الإسراء", 111, listOf(15)),
        com.example.model.SurahInfo(18, "الكهف", 110, listOf(15, 16)),
        com.example.model.SurahInfo(19, "مريم", 98, listOf(16)),
        com.example.model.SurahInfo(20, "طه", 135, listOf(16)),
        com.example.model.SurahInfo(21, "الأنبياء", 112, listOf(17)),
        com.example.model.SurahInfo(22, "الحج", 78, listOf(17)),
        com.example.model.SurahInfo(23, "المؤمنون", 118, listOf(18)),
        com.example.model.SurahInfo(24, "النور", 64, listOf(18)),
        com.example.model.SurahInfo(25, "الفرقان", 77, listOf(18, 19)),
        com.example.model.SurahInfo(26, "الشعراء", 227, listOf(19)),
        com.example.model.SurahInfo(27, "النمل", 93, listOf(19, 20)),
        com.example.model.SurahInfo(28, "القصص", 88, listOf(20)),
        com.example.model.SurahInfo(29, "العنكبوت", 69, listOf(20, 21)),
        com.example.model.SurahInfo(30, "الروم", 60, listOf(21)),
        com.example.model.SurahInfo(31, "لقمان", 34, listOf(21)),
        com.example.model.SurahInfo(32, "السجدة", 30, listOf(21)),
        com.example.model.SurahInfo(33, "الأحزاب", 73, listOf(21, 22)),
        com.example.model.SurahInfo(34, "سبأ", 54, listOf(22)),
        com.example.model.SurahInfo(35, "فاطر", 45, listOf(22)),
        com.example.model.SurahInfo(36, "يس", 83, listOf(22, 23)),
        com.example.model.SurahInfo(37, "الصافات", 182, listOf(23)),
        com.example.model.SurahInfo(38, "ص", 88, listOf(23)),
        com.example.model.SurahInfo(39, "الزمر", 75, listOf(23, 24)),
        com.example.model.SurahInfo(40, "غافر", 85, listOf(24)),
        com.example.model.SurahInfo(41, "فصلت", 54, listOf(24, 25)),
        com.example.model.SurahInfo(42, "الشورى", 53, listOf(25)),
        com.example.model.SurahInfo(43, "الزخرف", 89, listOf(25)),
        com.example.model.SurahInfo(44, "الدخان", 59, listOf(25)),
        com.example.model.SurahInfo(45, "الجاثية", 37, listOf(25)),
        com.example.model.SurahInfo(46, "الأحقاف", 35, listOf(26)),
        com.example.model.SurahInfo(47, "محمد", 38, listOf(26)),
        com.example.model.SurahInfo(48, "الفتح", 29, listOf(26)),
        com.example.model.SurahInfo(49, "الحجرات", 18, listOf(26)),
        com.example.model.SurahInfo(50, "ق", 45, listOf(26)),
        com.example.model.SurahInfo(51, "الذاريات", 60, listOf(26, 27)),
        com.example.model.SurahInfo(52, "الطور", 49, listOf(27)),
        com.example.model.SurahInfo(53, "النجم", 62, listOf(27)),
        com.example.model.SurahInfo(54, "القمر", 55, listOf(27)),
        com.example.model.SurahInfo(55, "الرحمن", 78, listOf(27)),
        com.example.model.SurahInfo(56, "الواقعة", 96, listOf(27)),
        com.example.model.SurahInfo(57, "الحديد", 29, listOf(27)),
        com.example.model.SurahInfo(58, "المجادلة", 22, listOf(28)),
        com.example.model.SurahInfo(59, "الحشر", 24, listOf(28)),
        com.example.model.SurahInfo(60, "الممتحنة", 13, listOf(28)),
        com.example.model.SurahInfo(61, "الصف", 14, listOf(28)),
        com.example.model.SurahInfo(62, "الجمعة", 11, listOf(28)),
        com.example.model.SurahInfo(63, "المنافقون", 11, listOf(28)),
        com.example.model.SurahInfo(64, "التغابن", 18, listOf(28)),
        com.example.model.SurahInfo(65, "الطلاق", 12, listOf(28)),
        com.example.model.SurahInfo(66, "التحريم", 12, listOf(28)),
        com.example.model.SurahInfo(67, "الملك", 30, listOf(29)),
        com.example.model.SurahInfo(68, "القلم", 52, listOf(29)),
        com.example.model.SurahInfo(69, "الحاقة", 52, listOf(29)),
        com.example.model.SurahInfo(70, "المعارج", 44, listOf(29)),
        com.example.model.SurahInfo(71, "نوح", 28, listOf(29)),
        com.example.model.SurahInfo(72, "الجن", 28, listOf(29)),
        com.example.model.SurahInfo(73, "المزمل", 20, listOf(29)),
        com.example.model.SurahInfo(74, "المدثر", 56, listOf(29)),
        com.example.model.SurahInfo(75, "القيامة", 40, listOf(29)),
        com.example.model.SurahInfo(76, "الإنسان", 31, listOf(29)),
        com.example.model.SurahInfo(77, "المرسلات", 50, listOf(29)),
        com.example.model.SurahInfo(78, "النبأ", 40, listOf(30)),
        com.example.model.SurahInfo(79, "النازعات", 46, listOf(30)),
        com.example.model.SurahInfo(80, "عبس", 42, listOf(30)),
        com.example.model.SurahInfo(81, "التكوير", 29, listOf(30)),
        com.example.model.SurahInfo(82, "الانفطار", 19, listOf(30)),
        com.example.model.SurahInfo(83, "المطففين", 36, listOf(30)),
        com.example.model.SurahInfo(84, "الانشقاق", 25, listOf(30)),
        com.example.model.SurahInfo(85, "البروج", 22, listOf(30)),
        com.example.model.SurahInfo(86, "الطارق", 17, listOf(30)),
        com.example.model.SurahInfo(87, "الأعلى", 19, listOf(30)),
        com.example.model.SurahInfo(88, "الغاشية", 26, listOf(30)),
        com.example.model.SurahInfo(89, "الفجر", 30, listOf(30)),
        com.example.model.SurahInfo(90, "البلد", 20, listOf(30)),
        com.example.model.SurahInfo(91, "الشمس", 15, listOf(30)),
        com.example.model.SurahInfo(92, "الليل", 21, listOf(30)),
        com.example.model.SurahInfo(93, "الضحى", 11, listOf(30)),
        com.example.model.SurahInfo(94, "الشرح", 8, listOf(30)),
        com.example.model.SurahInfo(95, "التين", 8, listOf(30)),
        com.example.model.SurahInfo(96, "العلق", 19, listOf(30)),
        com.example.model.SurahInfo(97, "القدر", 5, listOf(30)),
        com.example.model.SurahInfo(98, "البينة", 8, listOf(30)),
        com.example.model.SurahInfo(99, "الزلزلة", 8, listOf(30)),
        com.example.model.SurahInfo(100, "العاديات", 11, listOf(30)),
        com.example.model.SurahInfo(101, "القارعة", 11, listOf(30)),
        com.example.model.SurahInfo(102, "التكاثر", 8, listOf(30)),
        com.example.model.SurahInfo(103, "العصر", 3, listOf(30)),
        com.example.model.SurahInfo(104, "الهمزة", 9, listOf(30)),
        com.example.model.SurahInfo(105, "الفيل", 5, listOf(30)),
        com.example.model.SurahInfo(106, "قريش", 4, listOf(30)),
        com.example.model.SurahInfo(107, "الماعون", 7, listOf(30)),
        com.example.model.SurahInfo(108, "الكوثر", 3, listOf(30)),
        com.example.model.SurahInfo(109, "الكافرون", 6, listOf(30)),
        com.example.model.SurahInfo(110, "النصر", 3, listOf(30)),
        com.example.model.SurahInfo(111, "المسد", 5, listOf(30)),
        com.example.model.SurahInfo(112, "الإخلاص", 4, listOf(30)),
        com.example.model.SurahInfo(113, "الفلق", 5, listOf(30)),
        com.example.model.SurahInfo(114, "الناس", 6, listOf(30))
    )

    fun surahsForJuz(juzSet: Set<Int>): List<com.example.model.SurahInfo> =
        if (juzSet.isEmpty()) allSurahs
        else allSurahs.filter { surah -> surah.juzList.any { it in juzSet } }
}
