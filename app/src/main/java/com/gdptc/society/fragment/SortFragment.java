package com.gdptc.society.fragment;

/**
 * @创建者 Administrator
 * @创建时间 2016/4/11 10:39
 * @描述 ${TODO}
 * @更新者 $Author$
 * @更新时间 $Date$
 * @更新描述 ${TODO}
 */
public class SortFragment extends BaseFragment {
    String catalogues[][] = new String[][]{
            new String[]{"素拓分统计", "活动策划", "文案"},
            new String[]{"现场布置", "开幕仪式", "人员登记"},
            new String[]{"现场摄像", "活动后勤", "后期整理"},
            new String[]{"活动推广", "活动发布", "财务管理"},
            new String[]{"活动人员组织", "活动人员的管理", "活动人员的接待"},
            new String[]{"对外融资", "赞助拉拢", "资费筹集"},

    };
    String catalogues1[] = new String[]{"小美", "小瑜", "小芳", "晓东", "微微", "莉哥"};
    @Override
    protected String[][] setCatalogues2() {
        return catalogues;
    }

    @Override
    protected String[] setCatalogues1() {
        return catalogues1;
    }
}

