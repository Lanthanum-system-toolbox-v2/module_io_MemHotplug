import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import utils.NumAndBoolean;
import utils.StringToList;
import xzr.La.systemtoolbox.modules.java.LModule;
import xzr.La.systemtoolbox.ui.StandardCard;
import xzr.La.systemtoolbox.utils.process.ShellUtil;

import java.util.List;

public class MemHotplug implements LModule {
    static final String TAG="MemHotplug";
    final String node="/sys/devices/system/memory";

    AlertDialog dialog;
    List<String> mems;

    TextView meminfo;

    @Override
    public String classname() {
        return "io";
    }

    @Override
    public View init(Context context) {
        if(no_compatibility())
            return null;

        LinearLayout linearLayout=new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView title= StandardCard.title(context);
        title.setText("内存热插拔");
        linearLayout.addView(title);
        TextView subtitle = new TextView(context);
        subtitle.setText("您的内核支持内存热插拔，您可以选择性的休眠一些内存区块，从而节省电量。");
        linearLayout.addView(subtitle);

        meminfo=StandardCard.subtitle(context);
        meminfo.setText(genMemText(genMemTotalMiB()));
        linearLayout.addView(meminfo);

        mems= StringToList.to(ShellUtil.run("cd "+node+"\n" +
                "for i in memory*\n" +
                "do\n" +
                "if [ \"`cat ${i}/removable`\" == \"1\" ]\n" +
                "then\n" +
                "echo ${i}\n" +
                "fi\n" +
                "done\n",true));

        for(String name:mems){
            Switch sw=new Switch(context);
            final String online_node=genpath(name);
            sw.setText(name.replace("memory","内存区块"));
            sw.setChecked(NumAndBoolean.Num2Boolean(ShellUtil.run("cat "+online_node,true)));
            linearLayout.addView(sw);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LinearLayout dialog_view=new LinearLayout(context);
                    ProgressBar progressBar=new ProgressBar(context);
                    dialog_view.addView(progressBar);
                    TextView textView=new TextView(context);
                    textView.setText("正在处理");
                    dialog_view.addView(textView);

                    dialog=new AlertDialog.Builder(context)
                            .setTitle("请稍后")
                            .setView(dialog_view)
                            .setCancelable(false)
                            .create();
                    dialog.show();
                    new update("echo "+NumAndBoolean.Boolean2Num(((Switch)view).isChecked())+" > "+online_node).start();
                }
            });
        }



        return linearLayout;
    }
    String genpath(String name){
        return node+"/"+name+"/online";
    }
    String genMemText(int memsize){
        return "当前总内存："+memsize+"MB";
    }

    int genMemTotalMiB(){
        try{
            return Integer.parseInt(ShellUtil.run("MemTotalStr=`cat /proc/meminfo | grep MemTotal`\n" +
                    "MemTotal=${MemTotalStr:16:8}\n" +
                    "echo ${MemTotal}",true))/1024;
        }
        catch (Exception e){
            return 0;
        }
    }

    class update extends Thread{
        String cmd;
        public update(String cmd){
            this.cmd=cmd;
        }
        public void run(){
            ShellUtil.run(cmd,true);
            meminfo.setText(genMemText(genMemTotalMiB()));
            dialog.dismiss();
        }
    }

    boolean no_compatibility(){
        if(!ShellUtil.run("if [ -d "+node+" ]\nthen\necho true\nfi\n",true).equals("true"))
            return true;
        return false;
    }

    @Override
    public String onBootApply() {
        if(no_compatibility())
            return null;

        String cmd="";
        for(String name:mems){
            String node=genpath(name);
            String current=ShellUtil.run("cat "+node,true);
            cmd+="echo "+current+" > "+node+"\n";
        }

        return cmd;
    }

    @Override
    public void onExit() {

    }
}
