package robot1.example1.ioiorobot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class MainActivity extends Activity {

	/** ###################### INTERFACE ############################### */
	ImageView controller, robot;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setValue();
		
		controller.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Intent _controller = new Intent(getApplicationContext(),
						Client.class);
				startActivity(_controller);
				finish();
				return false;
			}
		});
		
		robot.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Intent _robot = new Intent(getApplicationContext(),
						Server.class);
				startActivity(_robot);
				//finish();
				return false;
			}
		});

	}

	/** ###################### SET INTERFACE VALUE ############################### */
	private void setValue() {
		controller = (ImageView) findViewById(R.id.controller);
		robot = (ImageView) findViewById(R.id.robot);

	}

}
