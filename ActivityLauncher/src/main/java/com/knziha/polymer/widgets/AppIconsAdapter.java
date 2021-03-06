package com.knziha.polymer.widgets;


import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.GlobalOptions;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.knziha.polymer.BrowserActivity;
import com.knziha.polymer.R;
import com.knziha.polymer.Toastable_Activity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.knziha.polymer.Utils.CMN;

import java.util.ArrayList;
import java.util.List;

import static com.knziha.polymer.widgets.Utils.GrayBG;

@SuppressWarnings("rawtypes")
public class AppIconsAdapter extends RecyclerView.Adapter<AppIconsAdapter.ViewHolder> {
	public final BottomSheetDialog shareDialog;
	private final View bottomSheet;
    private final FlowTextView indicator;
    private TextPaint textPainter;
    private ArrayList<AppBean> list = new ArrayList<>();
    private View.OnClickListener itemClicker;
    private PackageManager pm;
	private int landScapeMode;
	
	public AppIconsAdapter(Toastable_Activity a) {
        textPainter = DescriptiveImageView.createTextPainter();
        shareDialog = new BottomSheetDialog(a);
		shareDialog.getWindow().setDimAmount(0.2f);
		Window win = shareDialog.getWindow();
		if(win!=null) {
			win.setDimAmount(0.2f);
			View decor = win.getDecorView();
			decor.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
					-> v.postDelayed(() -> {
				if(landScapeMode!=a.mConfiguration.orientation) {
					show(a);
				}
			}, 0));
		}
		shareDialog.tag = this;
        itemClicker = v1 -> {
			ViewHolder vh = (ViewHolder) v1.getTag();
			AppBean appBean = list.get(vh.position);
			Intent shareIntent = new Intent(appBean.intent);
			shareIntent.setComponent(new ComponentName(appBean.pkgName, appBean.appLauncherClassName));
			shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			a.startActivity(shareIntent);
			shareDialog.dismiss();
		};
        bottomSheet = View.inflate(a, R.layout.share_bottom_dialog, null);
        indicator = bottomSheet.findViewById(R.id.indicator);
        RecyclerView recyclerView = bottomSheet.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(a, 4));
        recyclerView.setRecycledViewPool(Utils.MaxRecyclerPool(35));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(this);

        shareDialog.setContentView(bottomSheet);
    }

    public void show(Toastable_Activity a) {
        shareDialog.show();
		Utils.TrimWindowWidth(shareDialog.getWindow(), a.dm);
		landScapeMode=a.mConfiguration.orientation;
        boolean landScape = landScapeMode==Configuration.ORIENTATION_LANDSCAPE;
        BottomSheetBehavior beh = shareDialog.getBehavior();
        beh.setState(landScape?BottomSheetBehavior.STATE_EXPANDED:BottomSheetBehavior.STATE_COLLAPSED);
        beh.setSkipCollapsed(landScape);
        int target = GlobalOptions.isDark?Color.WHITE:Color.BLACK;
        textPainter.setColor(target);
        indicator.setTextColor(target);
        indicator.setText(a.getResources().getString(R.string.share_link));
        bottomSheet.setBackground(GlobalOptions.isDark?GrayBG:null);
    }

    /** 获取单身图标 */
    public void pullAvailableApps(Toastable_Activity a, String url, String text) {
		list.clear();
		boolean shareLink=text==null;
		Intent intent;
        if(shareLink) {
            intent=new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        } else {
            intent=new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.setType("text/plain");
        }
        pm = a.getPackageManager();
        ResolveResolvedQuery(intent);
        if(shareLink) {
			intent=new Intent(intent);
			intent.setAction(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_TEXT, url);
			intent.putExtra(Intent.EXTRA_HTML_TEXT, "FUCK");
			if(a instanceof BrowserActivity) {
				intent.putExtra(Intent.EXTRA_TITLE, ((BrowserActivity)a).currentWebView.getTitle());
			}
			intent.setType("text/plain");
			ResolveResolvedQuery(intent);
		}
        notifyDataSetChanged();
        show(a);
    }
	
	private void ResolveResolvedQuery(Intent intent) {
		List<ResolveInfo> resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
		for (ResolveInfo RinfoI : resolved) {
			AppBean appBean = new AppBean();
			appBean.intent = intent;
			appBean.data = RinfoI;
			appBean.pkgName = RinfoI.activityInfo.packageName;
			appBean.appLauncherClassName = RinfoI.activityInfo.name;
			list.add(appBean);
		}
	}
	
	@Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //CMN.Log("AppIconsAdapter::onCreateViewHolder");
        ViewHolder ret = new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.share_recycler_item, parent, false));
        ret.itemView.setOnClickListener(itemClicker);
        ret.textImageView.textPainter=textPainter;
        ret.textImageView.bNeedShadow=true;
        return ret;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.position = position;
        AppBean app = list.get(position);
        app.load();
        DescriptiveImageView iv = holder.textImageView;
        iv.setImageDrawable(app.icon);
        iv.setText(app.appName);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public int position;
        public DescriptiveImageView textImageView;
        public ViewHolder(View itemView) {
            super(itemView);
            textImageView = itemView.findViewById(R.id.app_icon_iv);
            itemView.setTag(this);
        }
    }

    public class AppBean {
        public Intent intent;
        public ResolveInfo data;
        public Drawable icon;
        public String appName;
        public String pkgName;
        public String appLauncherClassName;
        public boolean loaded;

        public void load() {
            if(!loaded) {
                appName = data.loadLabel(pm).toString();
                icon = data.loadIcon(pm);
                loaded=true;
            }
        }
    }
}