package com.d27.locator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.os.PersistableBundle;

public class LocatrActivity extends SingleFragmentActivity {

    @Override
    Fragment createFragment() {
        return LocatrFragment.newInstance();
    }
}
