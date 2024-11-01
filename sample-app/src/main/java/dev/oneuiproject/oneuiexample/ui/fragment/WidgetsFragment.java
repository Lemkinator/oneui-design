package dev.oneuiproject.oneuiexample.ui.fragment;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.SeslMenuItem;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import com.sec.sesl.tester.R;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneuiexample.ui.activity.AboutActivity;
import dev.oneuiproject.oneuiexample.ui.activity.MainActivity;
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment;

public class WidgetsFragment extends BaseFragment
        implements View.OnClickListener {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int[] Ids = {R.id.fragment_btn_1,
                R.id.fragment_btn_2,
                R.id.fragment_btn_3,
                R.id.fragment_btn_4,
                R.id.fragment_btn_5};
        for (int id : Ids) view.findViewById(id).setOnClickListener(this);

        AppCompatSpinner spinner = view.findViewById(R.id.fragment_spinner);
        List<String> items = new ArrayList<>();
        for (int i = 1; i < 5; i++)
            items.add("Spinner Item " + i);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        SearchView searchView = view.findViewById(R.id.fragment_searchview);
        SearchManager manager = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(manager.getSearchableInfo(
                new ComponentName(mContext, MainActivity.class)));
        searchView.seslSetUpButtonVisibility(View.VISIBLE);
        searchView.seslSetOnUpButtonClickListener(this);

        if (!isHidden()){
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.STARTED);
        }
    }

    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_widgets;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_game_launcher;
    }

    @Override
    public CharSequence getTitle() {
        return "Widgets";
    }

    @Override
    public void onClick(View v) {
        // no-op
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.STARTED);
        }else{
            requireActivity().removeMenuProvider(menuProvider);
        }
    }

    private MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_widgets, menu);

            DrawerLayout drawerLayout = ((MainActivity)getActivity()).getDrawerLayout();
            MenuItem searchItem = menu.findItem(R.id.menu_about_app);
            drawerLayout.setMenuItemBadge((SeslMenuItem) searchItem, new ToolbarLayout.Badge.Dot());
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_about_app) {
                startActivity(new Intent(requireActivity(), AboutActivity.class));
                return true;
            }
            return false;
        }
    };

}