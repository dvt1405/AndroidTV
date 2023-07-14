package com.kt.apps.core

object Constants {
    const val SHARE_PREF_NAME = "extra:default_share_pref_name"
    const val SHARE_PREF_DEFAULT = "extra:default_share_pref"

    const val USER_AGENT: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"

    const val KEY_ACCESS_TOKEN = "key:access_token"

    const val URL = "https://binhluan.90phut6.live/"

    const val DEEPLINK_HOST = "xemtivihd.net"

    const val HOST_TV = "tv"
    const val HOST_FOOTBALL = "bongda"
    const val HOST_RADIO = "radio"
    const val SCHEME_DEFAULT = "xemtv"

    const val EXTRA_PLAYBACK_TYPE = "extra:playback_type"
    const val EXTRA_FOOTBALL_MATCH = "extra:football_match"
    const val EXTRA_TV_CHANNEL = "extra:tv_channel"

    const val EXTRA_KEY_VERSION_NEED_REFRESH = "version_need_refresh"
    const val EXTRA_KEY_USE_ONLINE = "use_online_data"
    const val EXTRA_KEY_ALLOW_INTERNATIONAL = "allow_international"

    val mapChannel: Map<String, String> by lazy {
        mapOf(
            "abcaustralia" to "icon_channel_australia_plus_16564921977.png",
            "angiang" to "icon_channel_an_giang_165655813369.png",
            "anninhtv" to "icon_channel_antv_165655612485.png",
            "asianfoodnetwork" to "icon_channel_asian_food_channel_1656492213422.png",
            "afn" to "icon_channel_asian_food_channel_1656492213422.png",
            "bàrịavũngtàu" to "icon_channel_ba_ria_vung_tau_165655617345.png",
            "bắcgiang" to "icon_channel_bac_giang_165655615343.png",
            "bắckạn" to "icon_channel_bac_kan_165655814567.png",
            "bạcliêu" to "icon_channel_bac_lieu_16565581548.png",
            "bắcninh" to "icon_channel_bac_ninh_16565581667.png",
            "bếntre" to "icon_channel_ben_tre_166518804636.png",
            "bìnhđịnh" to "icon_channel_binh_dinh_165655818203.png",
            "bìnhdương1" to "icon_channel_binh_duong_1_165655622878.png",
            "bìnhdương11" to "icon_channel_btv_11_1656556391452.png",
            "bìnhdương2" to "icon_channel_binh_duong_2_165655623765.png",
            "bìnhdương4" to "icon_channel_imovie_hd_165656515084.png",
            "bìnhdương6" to "icon_channel_btv6_1656556375172.png",
            "bìnhphước1" to "icon_channel_binh_phuoc_1_165655621391.png",
            "bìnhphước2" to "icon_channel_hometv_bptv2_165655622154.png",
            "bìnhthuận" to "icon_channel_binh_thuan_165647570229.png",
            "càmau" to "icon_channel_ca_mau_165655821089.png",
            "cầnthơ" to "icon_channel_can_tho_165655892661.png",
            "đànẵng1" to "icon_channel_da_nang_1_1656492987662.png",
            "đànẵng2" to "icon_channel_da_nang_2_1656492995882.png",
            "davinci" to "icon_channel_da_vinci_learning_1656556593842.png",
            "đắklắk" to "icon_channel_dak_lak_165655700424.png",
            "điệnbiên" to "icon_channel_dien_bien_1656558226232.png",
            "đồngnai1" to "icon_channel_dongnai1.png",
            "đồngnai2" to "icon_channel_dongnai2.png",
            "đồngtháp1" to "icon_channel_dong_thap_165655753932.png",
            "đồngtháp2" to "icon_channel_mien_tay_thdt2_1656669937382.png",
            "dw" to "icon_channel_dw_1656492345412.png",
            "france24english" to "icon_channel_france_24_1656493080452.png",
            "gialai" to "icon_channel_gia_lai_1656558891032.png",
            "hàgiang" to "icon_channel_ha_giang_165647603612.png",
            "hànam" to "icon_channel_ha_nam_165655890147.png",
            "hànội1" to "icon_channel_hanoi1.png",
            "hàtĩnh" to "icon_channel_ha_tinh_1656558262372.png",
            "hậugiang" to "icon_channel_hau_giang_165655824967.png",
            "hòabình" to "icon_channel_hoa_binh_1656557056552.png",
            "htvkey" to "icon_channel_htv4_1656476700552.png",
            "htvthểthao" to "icon_channel_htvc_the_thao_1656493154832.png",
            "htv1" to "icon_channel_htv1_1656557069452.png",
            "htv2" to "icon_channel_htv2_165657612334.png",
            "htv3" to "icon_channel_htv3_1656558279912.png",
            "htv7" to "icon_channel_htv7_hd_16565582880.png",
            "htv9" to "icon_channel_htv9_hd_165655829886.png",
            "htvccanhạc" to "icon_channel_htvc_ca_nhac_1656493098162.png",
            "htvcdulịchcuộcsống" to "icon_channel_htvc_du_lich_1656493038942.png",
            "htvcgiađình" to "icon_channel_htvc_gia_dinh_1656493090842.png",
            "htvcmuasắmtiêudùng" to "icon_channel_htvc_shopping_1656493140532.png",
            "htvcphim" to "icon_channel_htvc_phim_1656493107552.png",
            "htvcphụnữ" to "icon_channel_htvc_phu_nu_1656493249982.png",
            "htvcthuầnviệt" to "icon_channel_htvc_thuan_viet_hd_1656478671262.png",
            "htvc+" to "icon_channel_htvc_plus_1656576133242.png",
            "kbsworld" to "icon_channel_kbs_world_1656557264112.png",
            "khánhhòa" to "icon_channel_khanh_hoa_1656558313512.png",
            "kix" to "icon_channel_kix_165649318532.png",
            "kontum" to "icon_channel_kon_tum_1656558326972.png",
            "lâmđồng" to "icon_channel_lam_dong_1656492399292.png",
            "lạngsơn" to "icon_channel_lang_son_1656585276672.png",
            "longan" to "icon_channel_long_an_1656558338422.png",
            "namđịnh" to "icon_channel_nam_dinh_1656557300672.png",
            "nghệan" to "icon_channel_nghe_an_1656493226672.png",
            "nhândântv" to "icon_channel_truyen_hinh_nhan_dan_165655731781.png",
            "nhkworld" to "icon_channel_nhk_world_hd_1656557328232.png",
            "ninhbình" to "icon_channel_ninh_binh_165649260222.png",
            "ninhthuận" to "icon_channel_ninh_thuan_1656557352692.png",
            "outdoorchannel" to "icon_channel_outdoor_channel_1656493235452.png",
            "quảngbình" to "icon_channel_quang_binh_tv_1656558608352.png",
            "quảngnam" to "icon_channel_quang_nam_1656492517622.png",
            "quảngngãi" to "icon_channel_quang_ngai_165655742091.png",
            "quảngninh1" to "icon_channel_quang_ninh_1_165655862222.png",
            "quảngninh3" to "icon_channel_quang_ninh_3_1656560399132.png",
            "quảngtrị" to "icon_channel_quang_tri_1656478574822.png",
            "quốchội" to "icon_channel_quoc_hoi_hd_165656647452.png",
            "quốcphòng" to "icon_channel_quoc_phong_vn_165647856201.png",
            "sóctrăng" to "icon_channel_soc_trang_165656082452.png",
            "tâyninh" to "icon_channel_tay_ninh_hd_1656478594032.png",
            "ththôngtấn" to "icon_channel_thong_tan_xa_viet_nam_165649329873.png",
            "tháibình" to "icon_channel_thai_binh_1656478612362.png",
            "tháinguyên" to "icon_channel_thai_nguyen_1656557523932.png",
            "thanhhóa" to "icon_channel_thanh_hoa_1656570355912.png",
            "thừathiênhuế" to "icon_channel_thua_thien_hue_1656558944262.png",
            "tiềngiang" to "icon_channel_tien_giang_1656557556552.png",
            "tràvinh" to "icon_channel_tra_vinh_1656557568522.png",
            "tuyênquang" to "icon_channel_tuyen_quang_1656560417792.png",
            "vĩnhlong1" to "icon_channel_thvl1_165657384745.png",
            "vĩnhlong2" to "icon_channel_vinh_long_2_1656573856152.png",
            "vĩnhlong3" to "icon_channel_vinh_long_3_1656573863882.png",
            "vĩnhlong4" to "icon_channel_vinh_long_4_hd_1656573877542.png",
            "vinhlong1" to "icon_channel_thvl1_165657384745.png",
            "vinhlong2" to "icon_channel_vinh_long_2_1656573856152.png",
            "vinhlong3" to "icon_channel_vinh_long_3_1656573863882.png",
            "vinhlong4" to "icon_channel_vinh_long_4_hd_1656573877542.png",
            "thvl1" to "icon_channel_thvl1_165657384745.png",
            "thvl2" to "icon_channel_vinh_long_2_1656573856152.png",
            "thvl3" to "icon_channel_vinh_long_3_1656573863882.png",
            "thvl4" to "icon_channel_vinh_long_4_hd_1656573877542.png",
            "vĩnhphúc" to "icon_channel_vinh_phuc_1656492619662.png",
            "vtc1" to "icon_channel_vtc1_1656493309872.png",
            "vtc10" to "icon_channel_vtc10_166706321017.jpg",
            "vtc11" to "icon_channel_vtc11_thieu_nhi_1656494361482.png",
            "vtc12" to "icon_channel_vtc12_165649436922.png",
            "vtc13" to "icon_channel_vtc13_hd_1656493465022.png",
            "vtc14" to "icon_channel_vtc14_hd_1656493581722.png",
            "vtc16" to "icon_channel_vtc16_1656493594572.png",
            "vtc2" to "icon_channel_vtc2_1656493319822.png",
            "vtc3" to "icon_channel_vtc3_hd_1656493328642.png",
            "vtc4" to "icon_channel_yeah1_family_vtc4_1656496937772.png",
            "vtc5" to "icon_channel_vtc5_1656496946342.png",
            "vtc6" to "icon_channel_vtc6_1656494338272.png",
            "vtc7" to "icon_channel_todaytv_vtc7_1656496956272.png",
            "vtc8" to "icon_channel_vtc8_1656494345432.png",
            "vtc9" to "icon_channel_vtc9_1656493473382.png",
            "vtvcầnthơ" to "icon_channel_vtv_can_tho_166537074507.jpg",
            "vtv6" to "icon_channel_vtv_can_tho_166537074507.jpg",
            "vtv1" to "icon_channel_vtv1_hd_165657381026.png",
            "vtv2" to "icon_channel_vtv2_hd_16565576783.png",
            "vtv3" to "icon_channel_vtv3_hd_165657381668.png",
            "vtv4" to "icon_channel_vtv4_hd_165657382285.png",
            "vtv5" to "icon_channel_vtv5_hd_165657382858.png",
            "vtv5tn" to "icon_channel_vtv5_tn.png",
            "vtv5hdtn" to "icon_channel_vtv5_tn.png",
            "vtv5tnb" to "icon_channel_vtv5_tnb.png",
            "vtv5hdtnb" to "icon_channel_vtv5_tnb.png",
            "vtv7" to "icon_channel_vtv7_hd_165655773646.png",
            "vtv8" to "icon_channel_vtv8_hd_165657496167.png",
            "vtv9" to "icon_channel_vtv9_hd_165657032671.png",
            "yênbái" to "icon_channel_yen_bai_1656559097542.png",
            "vovfm89" to "icon_channel_vov_suckhoe.png",
            "vovgiaothônghànội" to "icon_channel_vov_giaothong.png",
            "vovgiaothônghcm" to "icon_channel_vov_giaothong.png",
            "vovmekong" to "icon_channel_vov_mekong.png",
            "vov1" to "icon_channel_vov1.png",
            "vov2" to "icon_channel_vov2.png",
            "vov3" to "icon_channel_vov3.png",
            "vovtv" to "icon_channel_vovtv_161070256824.jpg",
            "vov4đồngbằngsôngcửulong" to "icon_channel_vov_dbscl.png",
            "vov4hồchíminh" to "icon_channel_vov_hcm.png",
            "vov4miềntrung" to "icon_channel_vov_mientrung.png",
            "vov4tâybắc" to "icon_channel_vov_taybac.png",
            "vov4đôngbắc" to "icon_channel_vov_dongbac.png",
            "vov4tâynguyên" to "icon_channel_vov_taynguyen.png",
            "vov5" to "icon_channel_vov5.png",
            "vov5chinese" to "icon_channel_vov5.png",
            "vov5english247" to "icon_channel_vov5.png",
            "vov5french" to "icon_channel_vov5.png",
            "vov5german" to "icon_channel_vov5.png",
            "vov5indonesian" to "icon_channel_vov5.png",
            "vov5japanese" to "icon_channel_vov5.png",
            "vov5khmer" to "icon_channel_vov5.png",
            "vov5laotian" to "icon_channel_vov5.png",
            "vov5russian" to "icon_channel_vov5.png",
            "vov5spanish" to "icon_channel_vov5.png",
            "vov5thailand" to "icon_channel_vov5.png",
            "vov6internationalsvc" to "icon_channel_vov6.png",
            "fm877mhz" to "icon_channel_voh.png",
            "fm956mhz" to "icon_channel_voh.png",
            "fm999mhz" to "icon_channel_voh.png",
            "am610khz" to "icon_channel_voh.png",
            "hànội2" to "icon_channel_hanoi2.png",
            "đồngnai3" to "icon_channel_dongnai3.png",
            "đắknông" to "icon_channel_daknong.png",
            "anviên" to "icon_channel_anvien.png",
            "htvcoop" to "icon_channel_htv_coop.png",
            "vtvcab2hd" to "icon_channel_vtvcab_phimviet7_1675158872.webp",
            "oncine1" to "icon_channel_vtvcab_oncine1_1675158860.webp",
            "vtvcaboncine1" to "icon_channel_vtvcab_oncine1_1675158860.webp",
            "ongold" to "icon_channel_vtvcab_on_gold1_1675158871.webp",
            "vtvcabongold" to "icon_channel_vtvcab_on_gold1_1675158871.webp",
            "onbibi" to "icon_channel_vtvcab_onbibi1_1675158864.webp",
            "tvtcabonbibi" to "icon_channel_vtvcab_onbibi1_1675158864.webp",
            "youtv" to "icon_channel_youtv_162952854632.jpg",
            "sctv1" to "icon_channel_sctv1.webp",
            "sctv2" to "icon_channel_sctv2",
            "sctv3" to "icon_channel_sctv3",
            "sctv4" to "icon_channel_sctv4",
            "sctv5" to "icon_channel_sctv5",
            "sctv6" to "icon_channel_sctv6",
            "sctv7" to "icon_channel_sctv7",
            "sctv8" to "icon_channel_sctv8",
            "sctv9" to "icon_channel_sctv9",
            "sctv10" to "icon_channel_sctv10",
            "sctv11" to "icon_channel_sctv11",
            "sctv12" to "icon_channel_sctv12",
            "sctv13" to "icon_channel_sctv13",
            "sctv14" to "icon_channel_sctv14",
            "sctv15" to "icon_channel_sctv15",
            "sctv17" to "icon_channel_sctv17",
            "sctv18" to "icon_channel_sctv18",
            "sctv19" to "icon_channel_sctv19",
            "sctv20" to "icon_channel_sctv20",
            "sctv21" to "icon_channel_sctv21",
            "sctv22" to "icon_channel_sctv22",
            "sctvpth" to "icon_channel_sctvphim.webp",
            "sctvphim" to "icon_channel_sctvphim.webp",
            "sctvphimtonghop" to "icon_channel_sctvphim.webp",
            "quochoi" to "icon_channel_quoc_hoi_hd_165656647452.png",
            "antv" to "icon_channel_antv_165655612485.png",
            "quocphong" to "icon_channel_quoc_phong_vn_165647856201.png",
            "quocphongvn" to "icon_channel_quoc_phong_vn_165647856201.png",
            "qpvn" to "icon_channel_quoc_phong_vn_165647856201.png",
            "nhandan" to "icon_channel_truyen_hinh_nhan_dan_165655731781.png",
            "nhandantv" to "icon_channel_truyen_hinh_nhan_dan_165655731781.png",
            "ndtv" to "icon_channel_truyen_hinh_nhan_dan_165655731781.png",
            "animax" to "icon_channel_animax_165647567355.png",
            "animalplanet" to "icon_channel_animal_planet_1656492721972.png",
            "animal" to "icon_channel_animal_planet_1656492721972.png",
            "animeclassio" to "icon_channel_animeclassio.jpeg",
            "anvien" to "icon_channel_anvien.png",
            "arirang" to "icon_channel_arirang_15148903242.png",
            "ariang" to "icon_channel_arirang_15148903242.png",
            "asianfood" to "icon_channel_asian_food_channel_1656492213422.png",
            "anx" to "icon_channel_axn.webp",
            "babyfirst" to "icon_channel_baby_first_152332783386.png",
            "babytv" to "icon_channel_babytv.png",
            "bbcearth" to "icon_channel_bbc_earth_148379672898.jpg",
            "bbclife" to "icon_channel_bbc_lifestyle_148379742953.jpg",
            "bbclifestyle" to "icon_channel_bbc_lifestyle_148379742953.jpg",
            "bbcnews" to "icon_channel_bbc_news_156325644439.webp",
            "bbcnew" to "icon_channel_bbc_news_156325644439.webp",
            "bloomberg" to "icon_channel_bloomberg_146581277148.png",
            "blueantentertainment" to "icon_channel_blue_ant_entertainment_165649225527.png",
            "blueantextreme" to "icon_channel_blue_ant_extreme_165649226345.png",
            "extreme" to "icon_channel_blue_ant_extreme_165649226345.png",
            "blueantext" to "icon_channel_blueantext.jpg",
            "blueantent" to "icon_channel_blueantent.jpg",
            "bongdaviet" to "icon_channel_bongdaviet.png",
            "bóngđáviệt" to "icon_channel_bongdaviet.png",
            "boomerang" to "icon_channel_boomerang_152240335127.png",
            "cartoonnetwork" to "icon_channel_cartoon_network_165649227684.png",
            "cartoonnw" to "icon_channel_cartoon_network_165649227684.png",
            "cbeebies" to "icon_channel_cbeebies.jpeg",
            "bbccbeebies" to "icon_channel_cbeebies.jpeg",
            "cinemaworld" to "icon_channel_cinemaworld_165647572623.png",
            "cnn" to "icon_channel_cnn_146581309815.png",
            "contv" to "icon_channel_contv.jpeg",
            "con" to "icon_channel_contv.jpeg",
            "davincilearning" to "icon_channel_da_vinci_learning_1656556593842.png",
            "davinci" to "icon_channel_da_vinci_learning_1656556593842.png",
            "discovery" to "icon_channel_discovery_157259072379.png",
            "discoveryasia" to "icon_channel_discovery_asia_152332466749.png",
            "discoverya" to "icon_channel_discovery_asia_152332466749.png",
            "dreamworks" to "icon_channel_dreamworks.png",
            "drfit" to "icon_channel_drfit.webp",
            "dw" to "icon_channel_dw_1656492345412.png",
            "fashion" to "icon_channel_fashion_tv_165649235825.png",
            "fashiontv" to "icon_channel_fashion_tv_165649235825.png",
            "film360" to "icon_channel_fim360_163298589919.jpg",
            "phim360" to "icon_channel_fim360_163298589919.jpg",
            "360film" to "icon_channel_fim360_163298589919.jpg",
            "360phim" to "icon_channel_fim360_163298589919.jpg",
            "360phimtruyen" to "icon_channel_fim360_163298589919.jpg",
            "france24" to "icon_channel_france_24_1656493080452.png",
            "happykids" to "icon_channel_happykids.jpeg",
            "hbo" to "icon_channel_hbo_165647605697.png",
            "hollywoodclassics" to "icon_channel_holllywoodclassics.webp",
            "imovie" to "icon_channel_imovie_hd_165656515084.png",
            "kbsworld" to "icon_channel_kbs_world_1656557264112.png",
            "kbs" to "icon_channel_kbs_world_1656557264112.png",
            "kns" to "icon_channel_kns_165655713016.png",
            "channelnewsasia" to "icon_channel_channel_newsasia_1656492950912.png",
            "newsasia" to "icon_channel_channel_newsasia_1656492950912.png",
            "cna" to "icon_channel_channel_newsasia_1656492950912.png",
            "cnbc" to "icon_channel_cnbc_1656492959472.png",
        )
    }
    const val REGEX_VN_A = "[aáàảãạăắằẳẵặâấầẩẫậ]"
    const val REGEX_VN_E = "[eéèẻẽẹêếềểễệ]"
    const val REGEX_VN_D = "[đ]"
    const val REGEX_VN_I = "[íìỉĩị]"
    const val REGEX_VN_O = "[oóòỏõọôốồổỗộơớờởỡợ]"
    const val REGEX_VN_U = "[uúùủũụưứừửữự]"
    val regexHttp by lazy {
        Regex("[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)")
    }
}