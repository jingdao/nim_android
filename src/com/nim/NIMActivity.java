package com.nim;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.AbsoluteLayout;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.Gravity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.Window;
import android.view.MotionEvent;
import android.util.Log;
import android.os.Handler;
import java.lang.Math;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

public class NIMActivity extends Activity implements View.OnTouchListener
{
	int margin = 10;
	int width,height;
	AbsoluteLayout al;
	Random r = new Random();
	int numHeaps = r.nextInt(5) + 2;
	int[] stones_per_heap = new int[numHeaps];
	ArrayList<Integer>[] stone_x = new ArrayList[numHeaps];
	ArrayList<Integer>[] stone_y = new ArrayList[numHeaps];
	int circleRadius;
	int stoneRadius;
	ArrayList<ImageView>[] tmpViews = new ArrayList[numHeaps];
	int selectedHeap = -1;
	int selectedAlpha = 75;

	boolean playerTurn = true;
	boolean useAI;
	int ai_heap=0, ai_stones=1;
	private Handler handler = new Handler();
	private Runnable runnable;
	int blinkDuration = 300;
	int blinkPeriod = 0;
	int blinkLimit = 5;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
//		View decorView = getWindow().getDecorView();
//		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//		decorView.setSystemUiVisibility(uiOptions);
		al = (AbsoluteLayout) findViewById(R.id.layout1);
		Display display = getWindowManager().getDefaultDisplay();
		width = display.getWidth();
		height = display.getHeight()-getStatusBarHeight();
		findViewById(android.R.id.content).setOnTouchListener(this);
		Bundle b = getIntent().getExtras();
		this.useAI = b == null;
		Log.d("NIMActivity",useAI+"");

		for (int i=0;i<numHeaps;i++) {
			stone_x[i] = new ArrayList<Integer>();
			stone_y[i] = new ArrayList<Integer>();
			tmpViews[i] = new ArrayList<ImageView>();
		}

		circleRadius = getCircleRadius();
		stoneRadius = (int) ((2 / Math.sqrt(2) * circleRadius - 4 * margin) / 6);
		Log.d("NIMActivity","heaps "+numHeaps+" radius "+circleRadius);
		double angle = -Math.PI / 4;
		double angle_step = Math.PI * 2 / numHeaps;
		for (int i=0;i<numHeaps;i++) {
			double scale = getScaleFromOrigin(circleRadius, angle);
			int x = (int) (width/2 + scale * Math.cos(angle));
			int y = (int) (height/2 + scale * Math.sin(angle));
//			Log.d("NIMActivity","x y "+x+" "+y);
			ImageView i1 = new ImageView(this);
			i1.setLayoutParams(new AbsoluteLayout.LayoutParams(
				2*circleRadius,2*circleRadius,x-circleRadius,y-circleRadius));
			i1.setImageResource(R.drawable.circle);
			al.addView(i1);
			angle += angle_step;

			stones_per_heap[i] = r.nextInt(7) + 1;
		}

		runnable = new Runnable() {
			public void run() {
				if (blinkPeriod == blinkLimit) {
					blinkPeriod = 0;
					stones_per_heap[ai_heap] -= ai_stones;
					drawStones();
					if (!checkGameOver())
						playerTurn = true;
				} else {
					for (int j=0;j<ai_stones;j++) {
						ImageView iv = tmpViews[ai_heap].get(j);
						if (iv.getImageAlpha() == selectedAlpha)
							iv.setAlpha(255);
						else
							iv.setAlpha(selectedAlpha);
					}
					blinkPeriod++;
					handler.postDelayed(this,blinkDuration);
				}
			}
		};

		drawStones();
    }

	public void drawStones() {
		for (int i=0;i<numHeaps;i++) {
			for (View v : tmpViews[i])
				al.removeView(v);
			stone_x[i].clear();
			stone_y[i].clear();
			tmpViews[i].clear();
		}

		double angle = -Math.PI / 4;
		double angle_step = Math.PI * 2 / numHeaps;
		for (int i=0;i<numHeaps;i++) {
			double scale = getScaleFromOrigin(circleRadius, angle);
			int x = (int) (width/2 + scale * Math.cos(angle));
			int y = (int) (height/2 + scale * Math.sin(angle));
			angle += angle_step;

			int numBottomRow = stones_per_heap[i] / 2; 
			int numTopRow = stones_per_heap[i] - numBottomRow;
			for (int j=0;j<stones_per_heap[i];j++) {
				ImageView i2 = new ImageView(this);
				int xx,yy;
				if (stones_per_heap[i] == 1) {
					xx = x;
					yy = y;
					i2.setLayoutParams(new AbsoluteLayout.LayoutParams(
						2*stoneRadius,2*stoneRadius,xx-stoneRadius,yy-stoneRadius));
				} else if (j < numTopRow) {
					xx = (int)(x - circleRadius/Math.sqrt(2) + 2/Math.sqrt(2) * circleRadius / (numTopRow + 1) * (j + 1));
					yy = (int)(y - circleRadius/Math.sqrt(2) + 2/Math.sqrt(2) * circleRadius / 3);
					i2.setLayoutParams(new AbsoluteLayout.LayoutParams(
						2*stoneRadius,2*stoneRadius,xx-stoneRadius,yy-stoneRadius));
				} else {
					xx = (int)(x - circleRadius/Math.sqrt(2) + 2/Math.sqrt(2) * circleRadius / (numBottomRow + 1) * (j-numTopRow + 1));
					yy = (int)(y - circleRadius/Math.sqrt(2) + 4/Math.sqrt(2) * circleRadius / 3);
					i2.setLayoutParams(new AbsoluteLayout.LayoutParams(
						2*stoneRadius,2*stoneRadius,xx-stoneRadius,yy-stoneRadius));
				}
				stone_x[i].add(xx);
				stone_y[i].add(yy);
				i2.setImageResource(R.drawable.stone);
				al.addView(i2);
				tmpViews[i].add(i2);
			}
		}
	}

	public int getStatusBarHeight() { 
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		} 
		return result;
	}

	public int getCircleRadius() {
		int[] options = {90,80,75,70,65,60,55,50,45,40};
		for (int i=0;i<options.length;i++) {
			if (isRadiusValid(options[i]))
				return options[i];
		}
		return 0;
	}

	public boolean isRadiusValid(int circleRadius) {
		double angle = -Math.PI / 4;
		double angle_step = Math.PI * 2 / numHeaps;
		double[] circle_x = new double[numHeaps];
		double[] circle_y = new double[numHeaps];
		for (int i=0;i<numHeaps;i++) {
			double scale = getScaleFromOrigin(circleRadius, angle);
			circle_x[i] = width/2 + scale * Math.cos(angle);
			circle_y[i] = height/2 + scale * Math.sin(angle);
			angle += angle_step;
		}
		boolean valid = true;
		for (int i=0;i<numHeaps;i++) {
			for (int j=i+1;j<numHeaps;j++) {
				double distance = 0;
				distance += (circle_x[i] - circle_x[j]) * (circle_x[i] - circle_x[j]);
				distance += (circle_y[i] - circle_y[j]) * (circle_y[i] - circle_y[j]);
				if (distance < (2 * circleRadius + margin) * (2 * circleRadius + margin)) {
					valid = false;
					break;
				}
			}
			if (!valid)
				break;
		}
		return valid;
	}

	public double getScaleFromOrigin(int circleRadius, double angle) {
		double c1,c2;
		double cx =  Math.cos(angle);
		double sx =  Math.sin(angle);
		if (cx > 0)
			c1 = (width - circleRadius - margin - width/2) / cx;
		else
			c1 = (margin + circleRadius - width/2) / cx;
		if (sx > 0)
			c2 = (height - circleRadius - margin - height/2) / sx;
		else
			c2 = (margin + circleRadius - height/2) / sx;
		return Math.min(c1,c2);
	}

	public void getAIMove() {
		int result = 0;
		ArrayList<Integer> remainingHeaps = new ArrayList<Integer>();
		for (int i=0;i<numHeaps;i++) {
			if (stones_per_heap[i] > 0) {
				result ^= stones_per_heap[i]; //XOR all stones
				remainingHeaps.add(i);
			}
		}
		if (result == 0) { //losing position
			Collections.shuffle(remainingHeaps);
			ai_heap = remainingHeaps.get(0);
//			ai_stones = random.nextInt(stones_per_heap[ai_heap]) + 1;
			ai_stones = 1;
		} else { //winning position
			//get the leftmost bit set
			int bit_set;
			for (bit_set=8;bit_set>0;bit_set>>=1)
				if ((result & bit_set) > 0)
					break;
			//pick the heap with this bit set
			//pick #stones such that the result XORs to zero
			for (ai_heap=0;ai_heap<numHeaps;ai_heap++) {
				if ((stones_per_heap[ai_heap] & bit_set) > 0) {
					ai_stones = stones_per_heap[ai_heap] - (result ^ stones_per_heap[ai_heap]);
					break;
				}
			}
		}
		handler.postDelayed(runnable, blinkDuration);
	}

	public boolean checkGameOver() {
		int sum = 0;
		for (int i=0;i<numHeaps;i++)
			sum += stones_per_heap[i];
		if (sum == 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Game Over").setMessage(!playerTurn?"Computer wins!":"You win!");
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		} else return false;
	}

	public boolean onTouch(View v, MotionEvent event) {
		if (!playerTurn)
			return false;
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (selectedHeap == -1) {
				for (int i=0;i<numHeaps;i++) {
					for (int j=0;j<stones_per_heap[i];j++) {
						double dx = Math.abs(event.getX() - stone_x[i].get(j));
						double dy = Math.abs(event.getY() - stone_y[i].get(j));
						if (dx < stoneRadius+margin/2 && dy < stoneRadius+margin/2) {
							selectedHeap = i;
							tmpViews[i].get(j).setAlpha(selectedAlpha);
							break;
						}
					}
				}
			} else {
				for (int j=0;j<stones_per_heap[selectedHeap];j++) {
					double dx = Math.abs(event.getX() - stone_x[selectedHeap].get(j));
					double dy = Math.abs(event.getY() - stone_y[selectedHeap].get(j));
					if (dx < stoneRadius+margin/2 && dy < stoneRadius+margin/2) {
						tmpViews[selectedHeap].get(j).setAlpha(selectedAlpha);
						break;
					}
				}
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (selectedHeap >= 0) {
				int numExtracted = 0;
				for (ImageView u : tmpViews[selectedHeap])
					if (u.getImageAlpha()==selectedAlpha)
						numExtracted++;
				if (numExtracted > 0) {
					stones_per_heap[selectedHeap] -= numExtracted;
					drawStones();
				}
				selectedHeap = -1;
				if (!checkGameOver() && useAI) {
					playerTurn = false;
					getAIMove();
				}
			}
		}
		return true;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.undomenu:
				return true;
			case R.id.newgamemenu:
				Intent intent = getIntent();
				Bundle b = new Bundle();
				intent.putExtras(b);
				finish();
				startActivity(intent);
				return true;
			case R.id.newaimenu:
				Intent intent2 = getIntent();
				intent2.replaceExtras((Bundle)null);
				finish();
				startActivity(intent2);
				return true;
			case R.id.quitmenu:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
