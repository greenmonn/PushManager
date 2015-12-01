package kr.ac.kaist.nmsl.pushmanager.warning;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import kr.ac.kaist.nmsl.pushmanager.R;

/**
 * Created by wns349 on 2015-12-01.
 */
public class WarningLayout extends RelativeLayout{
    private WindowManager windowManager;
    private WarningLayout self;
    private Context context;

    public WarningLayout(Context c, ViewGroup root){
        super(c);
        this.context = c;
        this.self = this;

        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        inflate(context, R.layout.warning_layout, this);

        // Initialize buttons
        final Button btnOkay = (Button)findViewById(R.id.btn_ok);
        btnOkay.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Ok clicked", Toast.LENGTH_SHORT).show();
                windowManager.removeView(self);
            }
        });


        final Button btnIgnore = (Button)findViewById(R.id.btn_ignore);
        btnIgnore.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Ignore clicked", Toast.LENGTH_SHORT).show();
                windowManager.removeView(self);
            }
        });

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // super.setVisibility(View.GONE);
        return super.dispatchKeyEvent(event);
    }
}
