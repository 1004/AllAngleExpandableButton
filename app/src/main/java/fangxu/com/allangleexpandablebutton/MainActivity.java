package fangxu.com.allangleexpandablebutton;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import fangxu.com.library.refactor.AllAngleExpandableButton;
import fangxu.com.library.refactor.ButtonData;

/**
 * Created by dear33 on 2016/8/20.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createButton();
    }

    private void createButton() {
        AllAngleExpandableButton button = (AllAngleExpandableButton) findViewById(R.id.button_expandable);
        List<ButtonData> buttonDatas = new ArrayList<>();
        String[] str = {"ab", "cd", "ef", "gh"};
        for (int i = 0; i < 4; i++) {
            ButtonData buttonData = new ButtonData(false);
            buttonData.setText(str[i]).setPadding(2).setTextSizeSp(20).setButtonSizeDp(60);
            buttonDatas.add(buttonData);
        }
        button.setStartAngle(0).setEndAngle(90).setButtonDatas(buttonDatas);
    }
}
