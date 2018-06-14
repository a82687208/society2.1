package com.gdptc.society.fragment;

/**
 * @创建者 Administrator
 * @创建时间 2016/4/11 10:39
 * @描述 ${TODO}
 * @更新者 $Author$
 * @更新时间 $Date$
 * @更新描述 ${TODO}
 */
public class CatalogueFragment extends BaseFragment {
    String catalogues[][] = new String[][]{
            new String[]{"活动介绍", "活动地点", "活动人数", "报名时间", "活动时间", "已报名人数", "活动流程", "设置提醒", "活动报名", "活动福利"},
            new String[]{"活动介绍", "活动地点", "活动人数", "报名时间", "活动时间", "已报名人数", "活动流程", "设置提醒", "活动报名", "活动福利"},
            new String[]{"活动介绍", "活动地点", "活动人数", "报名时间", "活动时间", "已报名人数", "活动流程", "设置提醒", "活动报名", "活动福利"},
            new String[]{"活动介绍", "活动地点", "活动人数", "报名时间", "活动时间", "已报名人数", "活动流程", "设置提醒", "活动报名", "活动福利"},
            new String[]{"活动介绍", "活动地点", "活动人数", "报名时间", "活动时间", "已报名人数", "活动流程", "设置提醒", "活动报名", "活动福利"},
            new String[]{"活动介绍", "活动地点", "活动人数", "报名时间", "活动时间", "已报名人数", "活动流程", "设置提醒", "活动报名", "活动福利"},
            new String[]{"活动介绍", "活动地点", "活动人数", "报名时间", "活动时间", "已报名人数", "活动流程", "设置提醒", "活动报名", "活动福利"},
    };
    String catalogues1[] = new String[]{"新生杯", "魅力杯", "换届大会", "迎新晚会", "总结大会", "周年庆", "社团节"};

    @Override
    protected String[][] setCatalogues2() {
        return catalogues;
    }

    @Override
    protected String[] setCatalogues1() {
        return catalogues1;
    }

}
