package kr.ac.kaist.nmsl.pushmanager.push;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import kr.ac.kaist.nmsl.pushmanager.R;

/**
 * Created by wns349 on 2015-12-10.
 */
public class PushNewsLayout extends RelativeLayout{
    private PushNewsLayout self;
    private Context context;
    private String url;

    public PushNewsLayout(Context c, ViewGroup root){
        super(c);
        this.context = c;
        this.self = this;

        inflate(context, R.layout.push_news_layout, this);
    }

    public void setNewsTitle(String title){
        TextView txtTitle = (TextView)findViewById(R.id.txt_news_title);
        txtTitle.setText(title);
    }

    public void setNewsContent(String content){
        TextView txtContent = (TextView) findViewById(R.id.txt_news_content);
        txtContent.setText(content);
    }

    public void setNewsURL(final String url){
        this.url = url;
    }

    public void updateViews(String type){
        final EditText edtAnswer = (EditText) findViewById(R.id.edt_answer);
        Button btnOkay = (Button) findViewById(R.id.btn_news_okay);
        Button btnCancel = (Button) findViewById(R.id.btn_news_cancel);

        btnCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                self.setVisibility(View.GONE);
            }
        });

        if(type.equalsIgnoreCase("News")){
            btnOkay.setText("Read more...");
            edtAnswer.setVisibility(View.GONE);
            btnOkay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    self.setVisibility(View.GONE);
                }
            });
        } else {
            // Question
            btnOkay.setText("Send answer...");
            edtAnswer.setVisibility(View.VISIBLE);
            btnOkay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Your answer is: " + edtAnswer.getText().toString(), Toast.LENGTH_SHORT).show();
                    self.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }
}
