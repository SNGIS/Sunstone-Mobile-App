package com.sunstone.model;

import android.content.Context;
import android.view.View;

import com.sunstone.R;
import com.sunstone.lolan_db.LolanEntryAlert;
import com.sunstone.lolan_db.LolanEntryAlertMulti;

import java.util.ArrayList;
import java.util.List;

public class LolanDbData {

    private List<LolanEntryAlert> lolanDbEntries = new ArrayList<>();

    public static void fillDatabaseMulti(List<LolanEntryAlertMulti> lolanDbEntries, View view, Context context) {

        LolanEntryAlertMulti lolanDbEntry111 = new LolanEntryAlertMulti("uint16", 0, "1,1,1", new int[] {1,1,1}, view.findViewById(R.id.tv_lolanvars_111), context.getText(R.string.path_111));
        lolanDbEntries.add(lolanDbEntry111);
        LolanEntryAlertMulti lolanDbEntry112 = new LolanEntryAlertMulti("str23", 0,"1,1,2", new int[] {1,1,2}, view.findViewById(R.id.tv_lolanvars_112), context.getText(R.string.path_112));
        lolanDbEntries.add(lolanDbEntry112);
        LolanEntryAlertMulti lolanDbEntry115 = new LolanEntryAlertMulti("uint32",0, "1,1,5", new int[] {1,1,5}, view.findViewById(R.id.tv_lolanvars_115), context.getText(R.string.path_115));
        lolanDbEntries.add(lolanDbEntry115);
        LolanEntryAlertMulti lolanDbEntry116 = new LolanEntryAlertMulti("uint32",0, "1,1,6", new int[] {1,1,6}, view.findViewById(R.id.tv_lolanvars_116), context.getText(R.string.path_116));
        lolanDbEntries.add(lolanDbEntry116);
        LolanEntryAlertMulti lolanDbEntry120 = new LolanEntryAlertMulti("uint32",0, "1,2,0", new int[] {1,2,0}, view.findViewById(R.id.tv_lolanvars_120), context.getText(R.string.path_120));
        lolanDbEntries.add(lolanDbEntry120);
        LolanEntryAlertMulti lolanDbEntry131 = new LolanEntryAlertMulti("uint16", 0,"1,3,1", new int[] {1,3,1}, view.findViewById(R.id.tv_lolanvars_131), context.getText(R.string.path_131));
        lolanDbEntries.add(lolanDbEntry131);
        LolanEntryAlertMulti lolanDbEntry132 = new LolanEntryAlertMulti("uint16",0, "1,3,2", new int[] {1,3,2}, view.findViewById(R.id.tv_lolanvars_132), context.getText(R.string.path_132));
        lolanDbEntries.add(lolanDbEntry132);
        LolanEntryAlertMulti lolanDbEntry135 = new LolanEntryAlertMulti("uint16", 0,"1,3,5", new int[] {1,3,5}, view.findViewById(R.id.tv_lolanvars_135), context.getText(R.string.path_135));
        lolanDbEntries.add(lolanDbEntry135);

        /**
         * 2. Filling LoLaN DB with TAG Settings variables
         */
        LolanEntryAlertMulti lolanDbEntry210 = new LolanEntryAlertMulti("uint16", 2, "2,1,0", new int[] {2,1,0}, view.findViewById(R.id.tv_lolanvars_210), context.getText(R.string.path_210));
        lolanDbEntries.add(lolanDbEntry210);
        LolanEntryAlertMulti lolanDbEntry221 = new LolanEntryAlertMulti("uint16",2,  "2,2,1", new int[] {2,2,1}, view.findViewById(R.id.tv_lolanvars_221), context.getText(R.string.path_221));
        lolanDbEntries.add(lolanDbEntry221);
        LolanEntryAlertMulti lolanDbEntry222 = new LolanEntryAlertMulti("uint8", 2, "2,2,2", new int[] {2,2,2}, view.findViewById(R.id.tv_lolanvars_222), context.getText(R.string.path_222));
        lolanDbEntries.add(lolanDbEntry222);
        LolanEntryAlertMulti lolanDbEntry223 = new LolanEntryAlertMulti("uint32",2,  "2,2,3", new int[] {2,2,3}, view.findViewById(R.id.tv_lolanvars_223), context.getText(R.string.path_223));
        lolanDbEntries.add(lolanDbEntry223);
        LolanEntryAlertMulti lolanDbEntry230 = new LolanEntryAlertMulti("uint32",2,  "2,3,0", new int[] {2,3,0}, view.findViewById(R.id.tv_lolanvars_230), context.getText(R.string.path_230));
        lolanDbEntries.add(lolanDbEntry230);
        LolanEntryAlertMulti lolanDbEntry241 = new LolanEntryAlertMulti("uint16",2,  "2,4,1", new int[] {2,4,1}, view.findViewById(R.id.tv_lolanvars_241), context.getText(R.string.path_241));
        lolanDbEntries.add(lolanDbEntry241);
        LolanEntryAlertMulti lolanDbEntry242 = new LolanEntryAlertMulti("uint16",2,  "2,4,2", new int[] {2,4,2}, view.findViewById(R.id.tv_lolanvars_242), context.getText(R.string.path_242));
        lolanDbEntries.add(lolanDbEntry242);
        LolanEntryAlertMulti lolanDbEntry243 = new LolanEntryAlertMulti("uint8", 2, "2,4,3", new int[] {2,4,3}, view.findViewById(R.id.tv_lolanvars_243), context.getText(R.string.path_243));
        lolanDbEntries.add(lolanDbEntry243);
        LolanEntryAlertMulti lolanDbEntry244 = new LolanEntryAlertMulti("uint8",2,  "2,4,4", new int[] {2,4,4}, view.findViewById(R.id.tv_lolanvars_244), context.getText(R.string.path_244));
        lolanDbEntries.add(lolanDbEntry244);
        LolanEntryAlertMulti lolanDbEntry245 = new LolanEntryAlertMulti("uint8", 2, "2,4,5", new int[] {2,4,5}, view.findViewById(R.id.tv_lolanvars_245), context.getText(R.string.path_245));
        lolanDbEntries.add(lolanDbEntry245);
        LolanEntryAlertMulti lolanDbEntry250 = new LolanEntryAlertMulti("uint16", 2, "2,5,0", new int[] {2,5,0}, view.findViewById(R.id.tv_lolanvars_250), context.getText(R.string.path_250));
        lolanDbEntries.add(lolanDbEntry250);
        LolanEntryAlertMulti lolanDbEntry260 = new LolanEntryAlertMulti("uint8", 2, "2,6,0", new int[] {2,6,0}, view.findViewById(R.id.tv_lolanvars_260), context.getText(R.string.path_260));
        lolanDbEntries.add(lolanDbEntry260);


        /**
         * 3. Filling LoLaN DB with DW1000 Settings variables
         */
        LolanEntryAlertMulti lolanDbEntry310 = new LolanEntryAlertMulti("uint8", 2, "3,1,0", new int[] {3,1,0}, view.findViewById(R.id.tv_lolanvars_310), context.getText(R.string.path_310));
        lolanDbEntries.add(lolanDbEntry310);
        LolanEntryAlertMulti lolanDbEntry321 = new LolanEntryAlertMulti("uint8", 2, "3,2,1", new int[] {3,2,1}, view.findViewById(R.id.tv_lolanvars_321), context.getText(R.string.path_321));
        lolanDbEntries.add(lolanDbEntry321);
        LolanEntryAlertMulti lolanDbEntry322 = new LolanEntryAlertMulti("uint16", 2, "3,2,2", new int[] {3,2,2}, view.findViewById(R.id.tv_lolanvars_322), context.getText(R.string.path_322));
        lolanDbEntries.add(lolanDbEntry322);
        LolanEntryAlertMulti lolanDbEntry330 = new LolanEntryAlertMulti("uint16", 2, "3,3,0", new int[] {3,3,0}, view.findViewById(R.id.tv_lolanvars_330), context.getText(R.string.path_330));
        lolanDbEntries.add(lolanDbEntry330);
        LolanEntryAlertMulti lolanDbEntry340 = new LolanEntryAlertMulti("uint16", 2, "3,4,0", new int[] {3,4,0}, view.findViewById(R.id.tv_lolanvars_340), context.getText(R.string.path_341));
        lolanDbEntries.add(lolanDbEntry340);
        LolanEntryAlertMulti lolanDbEntry351 = new LolanEntryAlertMulti("uint8", 2, "3,5,1", new int[] {3,5,1}, view.findViewById(R.id.tv_lolanvars_351), context.getText(R.string.path_351));
        lolanDbEntries.add(lolanDbEntry351);
        LolanEntryAlertMulti lolanDbEntry352 = new LolanEntryAlertMulti("uint8", 2, "3,5,2", new int[] {3,5,2}, view.findViewById(R.id.tv_lolanvars_352), context.getText(R.string.path_352));
        lolanDbEntries.add(lolanDbEntry352);
        LolanEntryAlertMulti lolanDbEntry360 = new LolanEntryAlertMulti("uint8", 2, "3,6,0", new int[] {3,6,0}, view.findViewById(R.id.tv_lolanvars_360), context.getText(R.string.path_360));
        lolanDbEntries.add(lolanDbEntry360);

        /**
         * 4. Filling LoLaN DB with CONTROL variables
         */
        LolanEntryAlertMulti lolanDbEntry410 = new LolanEntryAlertMulti("uint8", 1, "4,1,0", new int[] {4,1,0}, view.findViewById(R.id.tv_lolanvars_410), context.getText(R.string.path_410));
        lolanDbEntries.add(lolanDbEntry410);
        LolanEntryAlertMulti lolanDbEntry421 = new LolanEntryAlertMulti("uint8",1,  "4,2,1", new int[] {4,2,1}, view.findViewById(R.id.tv_lolanvars_421), context.getText(R.string.path_421));
        lolanDbEntries.add(lolanDbEntry421);
        LolanEntryAlertMulti lolanDbEntry422 = new LolanEntryAlertMulti("uint8",1,  "4,2,2", new int[] {4,2,2}, view.findViewById(R.id.tv_lolanvars_422), context.getText(R.string.path_422));
        lolanDbEntries.add(lolanDbEntry422);

        /**
         * 5. Filling LoLaN DB with STATUS variables
         */
        LolanEntryAlertMulti lolanDbEntry510 = new LolanEntryAlertMulti("uint8", 0, "5,1,0", new int[] {5,1,0}, view.findViewById(R.id.tv_lolanvars_510), context.getText(R.string.path_510));
        lolanDbEntries.add(lolanDbEntry510);
        LolanEntryAlertMulti lolanDbEntry521 = new LolanEntryAlertMulti("uint16",0,  "5,2,1", new int[] {5,2,1}, view.findViewById(R.id.tv_lolanvars_521), context.getText(R.string.path_521));
        lolanDbEntries.add(lolanDbEntry521);
        LolanEntryAlertMulti lolanDbEntry522 = new LolanEntryAlertMulti("uint8", 0, "5,2,2", new int[] {5,2,2}, view.findViewById(R.id.tv_lolanvars_522), context.getText(R.string.path_522));
        lolanDbEntries.add(lolanDbEntry522);
        LolanEntryAlertMulti lolanDbEntry531 = new LolanEntryAlertMulti("int16", 0, "5,3,1", new int[] {5,3,1}, view.findViewById(R.id.tv_lolanvars_531), context.getText(R.string.path_531));
        lolanDbEntries.add(lolanDbEntry531);
        LolanEntryAlertMulti lolanDbEntry532 = new LolanEntryAlertMulti("int16", 0, "5,3,2", new int[] {5,3,2}, view.findViewById(R.id.tv_lolanvars_532), context.getText(R.string.path_532));
        lolanDbEntries.add(lolanDbEntry532);
        LolanEntryAlertMulti lolanDbEntry533 = new LolanEntryAlertMulti("int16", 0, "5,3,3", new int[] {5,3,3}, view.findViewById(R.id.tv_lolanvars_533), context.getText(R.string.path_533));
        lolanDbEntries.add(lolanDbEntry533);
        LolanEntryAlertMulti lolanDbEntry534 = new LolanEntryAlertMulti("uint8", 0, "5,3,4", new int[] {5,3,4}, view.findViewById(R.id.tv_lolanvars_534), context.getText(R.string.path_534));
        lolanDbEntries.add(lolanDbEntry534);
        LolanEntryAlertMulti lolanDbEntry535 = new LolanEntryAlertMulti("data80", 0, "5,3,5", new int[] {5,3,5}, view.findViewById(R.id.tv_lolanvars_535), context.getText(R.string.path_535));
        lolanDbEntries.add(lolanDbEntry535);
        LolanEntryAlertMulti lolanDbEntry540 = new LolanEntryAlertMulti("int8", 0, "5,4,0", new int[] {5,4,0}, view.findViewById(R.id.tv_lolanvars_540), context.getText(R.string.path_540));
        lolanDbEntries.add(lolanDbEntry540);
        LolanEntryAlertMulti lolanDbEntry550 = new LolanEntryAlertMulti("uint32", 0, "5,5,0", new int[] {5,5,0}, view.findViewById(R.id.tv_lolanvars_550), context.getText(R.string.path_550));
        lolanDbEntries.add(lolanDbEntry550);
        LolanEntryAlertMulti lolanDbEntry560 = new LolanEntryAlertMulti("uint32", 0, "5,6,0", new int[] {5,6,0}, view.findViewById(R.id.tv_lolanvars_560), context.getText(R.string.path_560));
        lolanDbEntries.add(lolanDbEntry560);
        LolanEntryAlertMulti lolanDbEntry571 = new LolanEntryAlertMulti("uint16", 0, "5,7,1", new int[] {5,7,1}, view.findViewById(R.id.tv_lolanvars_571), context.getText(R.string.path_571));
        lolanDbEntries.add(lolanDbEntry571);
        LolanEntryAlertMulti lolanDbEntry572 = new LolanEntryAlertMulti("uint16", 0, "5,7,2", new int[] {5,7,2}, view.findViewById(R.id.tv_lolanvars_572), context.getText(R.string.path_572));
        lolanDbEntries.add(lolanDbEntry572);

    }

    public static void fillDatabase(List<LolanEntryAlert> lolanDbEntries, View view, Context context) {

        LolanEntryAlert lolanDbEntry111 = new LolanEntryAlert("uint16", 0, "1,1,1", new int[] {1,1,1}, view.findViewById(R.id.tv_lolanvars_111), context.getText(R.string.path_111));
        lolanDbEntries.add(lolanDbEntry111);
        LolanEntryAlert lolanDbEntry112 = new LolanEntryAlert("str23", 0,"1,1,2", new int[] {1,1,2}, view.findViewById(R.id.tv_lolanvars_112), context.getText(R.string.path_112));
        lolanDbEntries.add(lolanDbEntry112);
        LolanEntryAlert lolanDbEntry115 = new LolanEntryAlert("uint32",0, "1,1,5", new int[] {1,1,5}, view.findViewById(R.id.tv_lolanvars_115), context.getText(R.string.path_115));
        lolanDbEntries.add(lolanDbEntry115);
        LolanEntryAlert lolanDbEntry116 = new LolanEntryAlert("uint32",0, "1,1,6", new int[] {1,1,6}, view.findViewById(R.id.tv_lolanvars_116), context.getText(R.string.path_116));
        lolanDbEntries.add(lolanDbEntry116);
        LolanEntryAlert lolanDbEntry120 = new LolanEntryAlert("uint32",0, "1,2,0", new int[] {1,2,0}, view.findViewById(R.id.tv_lolanvars_120), context.getText(R.string.path_120));
        lolanDbEntries.add(lolanDbEntry120);
        LolanEntryAlert lolanDbEntry131 = new LolanEntryAlert("uint16", 0,"1,3,1", new int[] {1,3,1}, view.findViewById(R.id.tv_lolanvars_131), context.getText(R.string.path_131));
        lolanDbEntries.add(lolanDbEntry131);
        LolanEntryAlert lolanDbEntry132 = new LolanEntryAlert("uint16",0, "1,3,2", new int[] {1,3,2}, view.findViewById(R.id.tv_lolanvars_132), context.getText(R.string.path_132));
        lolanDbEntries.add(lolanDbEntry132);
        LolanEntryAlert lolanDbEntry135 = new LolanEntryAlert("uint16", 0,"1,3,5", new int[] {1,3,5}, view.findViewById(R.id.tv_lolanvars_135), context.getText(R.string.path_135));
        lolanDbEntries.add(lolanDbEntry135);

        /**
         * 2. Filling LoLaN DB with TAG Settings variables
         */
        LolanEntryAlert lolanDbEntry210 = new LolanEntryAlert("uint16", 2, "2,1,0", new int[] {2,1,0}, view.findViewById(R.id.tv_lolanvars_210), context.getText(R.string.path_210));
        lolanDbEntries.add(lolanDbEntry210);
        LolanEntryAlert lolanDbEntry221 = new LolanEntryAlert("uint16",2,  "2,2,1", new int[] {2,2,1}, view.findViewById(R.id.tv_lolanvars_221), context.getText(R.string.path_221));
        lolanDbEntries.add(lolanDbEntry221);
        LolanEntryAlert lolanDbEntry222 = new LolanEntryAlert("uint8", 2, "2,2,2", new int[] {2,2,2}, view.findViewById(R.id.tv_lolanvars_222), context.getText(R.string.path_222));
        lolanDbEntries.add(lolanDbEntry222);
        LolanEntryAlert lolanDbEntry223 = new LolanEntryAlert("uint32",2,  "2,2,3", new int[] {2,2,3}, view.findViewById(R.id.tv_lolanvars_223), context.getText(R.string.path_223));
        lolanDbEntries.add(lolanDbEntry223);
        LolanEntryAlert lolanDbEntry230 = new LolanEntryAlert("uint32",2,  "2,3,0", new int[] {2,3,0}, view.findViewById(R.id.tv_lolanvars_230), context.getText(R.string.path_230));
        lolanDbEntries.add(lolanDbEntry230);
        LolanEntryAlert lolanDbEntry241 = new LolanEntryAlert("uint16",2,  "2,4,1", new int[] {2,4,1}, view.findViewById(R.id.tv_lolanvars_241), context.getText(R.string.path_241));
        lolanDbEntries.add(lolanDbEntry241);
        LolanEntryAlert lolanDbEntry242 = new LolanEntryAlert("uint16",2,  "2,4,2", new int[] {2,4,2}, view.findViewById(R.id.tv_lolanvars_242), context.getText(R.string.path_242));
        lolanDbEntries.add(lolanDbEntry242);
        LolanEntryAlert lolanDbEntry243 = new LolanEntryAlert("uint8", 2, "2,4,3", new int[] {2,4,3}, view.findViewById(R.id.tv_lolanvars_243), context.getText(R.string.path_243));
        lolanDbEntries.add(lolanDbEntry243);
        LolanEntryAlert lolanDbEntry244 = new LolanEntryAlert("uint8",2,  "2,4,4", new int[] {2,4,4}, view.findViewById(R.id.tv_lolanvars_244), context.getText(R.string.path_244));
        lolanDbEntries.add(lolanDbEntry244);
        LolanEntryAlert lolanDbEntry245 = new LolanEntryAlert("uint8", 2, "2,4,5", new int[] {2,4,5}, view.findViewById(R.id.tv_lolanvars_245), context.getText(R.string.path_245));
        lolanDbEntries.add(lolanDbEntry245);
        LolanEntryAlert lolanDbEntry250 = new LolanEntryAlert("uint16", 2, "2,5,0", new int[] {2,5,0}, view.findViewById(R.id.tv_lolanvars_250), context.getText(R.string.path_250));
        lolanDbEntries.add(lolanDbEntry250);
        LolanEntryAlert lolanDbEntry260 = new LolanEntryAlert("uint8", 2, "2,6,0", new int[] {2,6,0}, view.findViewById(R.id.tv_lolanvars_260), context.getText(R.string.path_260));
        lolanDbEntries.add(lolanDbEntry260);


        /**
         * 3. Filling LoLaN DB with DW1000 Settings variables
         */
        LolanEntryAlert lolanDbEntry310 = new LolanEntryAlert("uint8", 2, "3,1,0", new int[] {3,1,0}, view.findViewById(R.id.tv_lolanvars_310), context.getText(R.string.path_310));
        lolanDbEntries.add(lolanDbEntry310);
        LolanEntryAlert lolanDbEntry321 = new LolanEntryAlert("uint8", 2, "3,2,1", new int[] {3,2,1}, view.findViewById(R.id.tv_lolanvars_321), context.getText(R.string.path_321));
        lolanDbEntries.add(lolanDbEntry321);
        LolanEntryAlert lolanDbEntry322 = new LolanEntryAlert("uint16", 2, "3,2,2", new int[] {3,2,2}, view.findViewById(R.id.tv_lolanvars_322), context.getText(R.string.path_322));
        lolanDbEntries.add(lolanDbEntry322);
        LolanEntryAlert lolanDbEntry330 = new LolanEntryAlert("uint16", 2, "3,3,0", new int[] {3,3,0}, view.findViewById(R.id.tv_lolanvars_330), context.getText(R.string.path_330));
        lolanDbEntries.add(lolanDbEntry330);
        LolanEntryAlert lolanDbEntry340 = new LolanEntryAlert("uint16", 2, "3,4,0", new int[] {3,4,0}, view.findViewById(R.id.tv_lolanvars_340), context.getText(R.string.path_341));
        lolanDbEntries.add(lolanDbEntry340);
        LolanEntryAlert lolanDbEntry351 = new LolanEntryAlert("uint8", 2, "3,5,1", new int[] {3,5,1}, view.findViewById(R.id.tv_lolanvars_351), context.getText(R.string.path_351));
        lolanDbEntries.add(lolanDbEntry351);
        LolanEntryAlert lolanDbEntry352 = new LolanEntryAlert("uint8", 2, "3,5,2", new int[] {3,5,2}, view.findViewById(R.id.tv_lolanvars_352), context.getText(R.string.path_352));
        lolanDbEntries.add(lolanDbEntry352);
        LolanEntryAlert lolanDbEntry360 = new LolanEntryAlert("uint8", 2, "3,6,0", new int[] {3,6,0}, view.findViewById(R.id.tv_lolanvars_360), context.getText(R.string.path_360));
        lolanDbEntries.add(lolanDbEntry360);

        /**
         * 4. Filling LoLaN DB with CONTROL variables
         */
        LolanEntryAlert lolanDbEntry410 = new LolanEntryAlert("uint8", 1, "4,1,0", new int[] {4,1,0}, view.findViewById(R.id.tv_lolanvars_410), context.getText(R.string.path_410));
        lolanDbEntries.add(lolanDbEntry410);
        LolanEntryAlert lolanDbEntry421 = new LolanEntryAlert("uint8",1,  "4,2,1", new int[] {4,2,1}, view.findViewById(R.id.tv_lolanvars_421), context.getText(R.string.path_421));
        lolanDbEntries.add(lolanDbEntry421);
        LolanEntryAlert lolanDbEntry422 = new LolanEntryAlert("uint8",1,  "4,2,2", new int[] {4,2,2}, view.findViewById(R.id.tv_lolanvars_422), context.getText(R.string.path_422));
        lolanDbEntries.add(lolanDbEntry422);

        /**
         * 5. Filling LoLaN DB with STATUS variables
         */
        LolanEntryAlert lolanDbEntry510 = new LolanEntryAlert("uint8", 0, "5,1,0", new int[] {5,1,0}, view.findViewById(R.id.tv_lolanvars_510), context.getText(R.string.path_510));
        lolanDbEntries.add(lolanDbEntry510);
        LolanEntryAlert lolanDbEntry521 = new LolanEntryAlert("uint16",0,  "5,2,1", new int[] {5,2,1}, view.findViewById(R.id.tv_lolanvars_521), context.getText(R.string.path_521));
        lolanDbEntries.add(lolanDbEntry521);
        LolanEntryAlert lolanDbEntry522 = new LolanEntryAlert("uint8", 0, "5,2,2", new int[] {5,2,2}, view.findViewById(R.id.tv_lolanvars_522), context.getText(R.string.path_522));
        lolanDbEntries.add(lolanDbEntry522);
        LolanEntryAlert lolanDbEntry531 = new LolanEntryAlert("int16", 0, "5,3,1", new int[] {5,3,1}, view.findViewById(R.id.tv_lolanvars_531), context.getText(R.string.path_531));
        lolanDbEntries.add(lolanDbEntry531);
        LolanEntryAlert lolanDbEntry532 = new LolanEntryAlert("int16", 0, "5,3,2", new int[] {5,3,2}, view.findViewById(R.id.tv_lolanvars_532), context.getText(R.string.path_532));
        lolanDbEntries.add(lolanDbEntry532);
        LolanEntryAlert lolanDbEntry533 = new LolanEntryAlert("int16", 0, "5,3,3", new int[] {5,3,3}, view.findViewById(R.id.tv_lolanvars_533), context.getText(R.string.path_533));
        lolanDbEntries.add(lolanDbEntry533);
        LolanEntryAlert lolanDbEntry534 = new LolanEntryAlert("uint8", 0, "5,3,4", new int[] {5,3,4}, view.findViewById(R.id.tv_lolanvars_534), context.getText(R.string.path_534));
        lolanDbEntries.add(lolanDbEntry534);
        LolanEntryAlert lolanDbEntry535 = new LolanEntryAlert("data80", 0, "5,3,5", new int[] {5,3,5}, view.findViewById(R.id.tv_lolanvars_535), context.getText(R.string.path_535));
        lolanDbEntries.add(lolanDbEntry535);
        LolanEntryAlert lolanDbEntry540 = new LolanEntryAlert("int8", 0, "5,4,0", new int[] {5,4,0}, view.findViewById(R.id.tv_lolanvars_540), context.getText(R.string.path_540));
        lolanDbEntries.add(lolanDbEntry540);
        LolanEntryAlert lolanDbEntry550 = new LolanEntryAlert("uint32", 0, "5,5,0", new int[] {5,5,0}, view.findViewById(R.id.tv_lolanvars_550), context.getText(R.string.path_550));
        lolanDbEntries.add(lolanDbEntry550);
        LolanEntryAlert lolanDbEntry560 = new LolanEntryAlert("uint32", 0, "5,6,0", new int[] {5,6,0}, view.findViewById(R.id.tv_lolanvars_560), context.getText(R.string.path_560));
        lolanDbEntries.add(lolanDbEntry560);
        LolanEntryAlert lolanDbEntry571 = new LolanEntryAlert("uint16", 0, "5,7,1", new int[] {5,7,1}, view.findViewById(R.id.tv_lolanvars_571), context.getText(R.string.path_571));
        lolanDbEntries.add(lolanDbEntry571);
        LolanEntryAlert lolanDbEntry572 = new LolanEntryAlert("uint16", 0, "5,7,2", new int[] {5,7,2}, view.findViewById(R.id.tv_lolanvars_572), context.getText(R.string.path_572));
        lolanDbEntries.add(lolanDbEntry572);

    }
}
