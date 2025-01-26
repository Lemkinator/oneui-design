package dev.oneuiproject.oneuiexample.ui.main.fragment;

import com.sec.sesl.tester.R;

import dev.oneuiproject.oneuiexample.ui.main.core.base.BaseFragment;

public class QRCodeFragment extends BaseFragment {


    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_qr_code;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_qr_code;
    }

    @Override
    public CharSequence getTitle() {
        return "QRCode";
    }

}
