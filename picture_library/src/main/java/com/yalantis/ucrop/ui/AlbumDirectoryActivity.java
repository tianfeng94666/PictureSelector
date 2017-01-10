package com.yalantis.ucrop.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.yalantis.ucrop.R;
import com.yalantis.ucrop.adapter.AlbumDirectoryAdapter;
import com.yalantis.ucrop.decoration.RecycleViewDivider;
import com.yalantis.ucrop.entity.LocalMedia;
import com.yalantis.ucrop.entity.LocalMediaFolder;
import com.yalantis.ucrop.util.Constants;
import com.yalantis.ucrop.util.LocalMediaLoader;
import com.yalantis.ucrop.util.Options;
import com.yalantis.ucrop.util.ToolbarUtil;
import com.yalantis.ucrop.util.Utils;
import com.yalantis.ucrop.widget.PublicTitleBar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.ui
 * email：893855882@qq.com
 * data：16/12/31
 */
public class AlbumDirectoryActivity extends BaseActivity implements View.OnClickListener, PublicTitleBar.OnTitleBarClick, AlbumDirectoryAdapter.OnItemClickListener {

    private List<LocalMediaFolder> folders = new ArrayList<>();
    private AlbumDirectoryAdapter adapter;
    private RecyclerView recyclerView;
    private PublicTitleBar titleBar;
    private TextView tv_empty;
    private List<LocalMedia> medias = new ArrayList<>();


    public static void startPhoto(Activity activity, Options options) {
        if (options == null) {
            options = new Options();
        }
        Intent intent = new Intent(activity, AlbumDirectoryActivity.class);
        intent.putExtra(Constants.EXTRA_MAX_SELECT_NUM, options.getMaxSelectNum());
        intent.putExtra(Constants.EXTRA_CROP_MODE, options.getCopyMode());
        intent.putExtra(Constants.EXTRA_SELECT_MODE, options.getSelectMode());
        intent.putExtra(Constants.EXTRA_SHOW_CAMERA, options.isShowCamera());
        intent.putExtra(Constants.EXTRA_ENABLE_PREVIEW, options.isEnablePreview());
        intent.putExtra(Constants.EXTRA_ENABLE_CROP, options.isEnableCrop());
        intent.putExtra(Constants.EXTRA_TYPE, options.getType());
        intent.putExtra(Constants.EXTRA_ENABLE_PREVIEW_VIDEO, options.isPreviewVideo());
        intent.putExtra(Constants.BACKGROUND_COLOR, options.getThemeStyle());
        intent.putExtra(Constants.CHECKED_DRAWABLE, options.getCheckedBoxDrawable());
        intent.putExtra(Constants.EXTRA_CROP_W, options.getCropW());
        intent.putExtra(Constants.EXTRA_CROP_H, options.getCropH());
        intent.putExtra(Constants.EXTRA_COMPRESS, options.isCompress());
        activity.startActivityForResult(intent, Constants.REQUEST_IMAGE);
        activity.overridePendingTransition(R.anim.slide_bottom_in, 0);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        type = getIntent().getIntExtra(Constants.EXTRA_TYPE, 1);// 1 图片 2视频
        showCamera = getIntent().getBooleanExtra(Constants.EXTRA_SHOW_CAMERA, true);// 是否显示拍摄
        enablePreview = getIntent().getBooleanExtra(Constants.EXTRA_ENABLE_PREVIEW, true);// 是否显示预览
        selectMode = getIntent().getIntExtra(Constants.EXTRA_SELECT_MODE, Constants.MODE_MULTIPLE);// 选择模式,单选or多选
        enableCrop = getIntent().getBooleanExtra(Constants.EXTRA_ENABLE_CROP, false);// 是否裁剪
        maxSelectNum = getIntent().getIntExtra(Constants.EXTRA_MAX_SELECT_NUM, Constants.SELECT_MAX_NUM);// 图片最大选择数量
        copyMode = getIntent().getIntExtra(Constants.EXTRA_CROP_MODE, Constants.COPY_MODEL_DEFAULT);// 裁剪模式
        enablePreviewVideo = getIntent().getBooleanExtra(Constants.EXTRA_ENABLE_PREVIEW_VIDEO, false);// 是否预览视频
        backgroundColor = getIntent().getIntExtra(Constants.BACKGROUND_COLOR, 0);
        cb_drawable = getIntent().getIntExtra(Constants.CHECKED_DRAWABLE, 0);
        isCompress = getIntent().getBooleanExtra(Constants.EXTRA_COMPRESS, false);
        cropW = getIntent().getIntExtra(Constants.EXTRA_CROP_W, 0);
        cropH = getIntent().getIntExtra(Constants.EXTRA_CROP_H, 0);
        titleBar = (PublicTitleBar) findViewById(R.id.titleBar);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        tv_empty = (TextView) findViewById(R.id.tv_empty);
        tv_empty.setOnClickListener(this);

        switch (type) {
            case LocalMediaLoader.TYPE_IMAGE:
                titleBar.setTitleText(getString(R.string.all_photo));
                break;
            case LocalMediaLoader.TYPE_VIDEO:
                titleBar.setTitleText(getString(R.string.all_video));
                break;
        }
        ToolbarUtil.setColorNoTranslucent(this, backgroundColor);
        titleBar.setTitleBarBackgroundColor(backgroundColor);
        titleBar.setRightText(getString(R.string.cancel));
        titleBar.setOnTitleBarClickListener(this);
        adapter = new AlbumDirectoryAdapter(this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new RecycleViewDivider(
                mContext, LinearLayoutManager.HORIZONTAL, Utils.dip2px(this, 0.5f), ContextCompat.getColor(this, R.color.line_color)));
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
        // 先判断手机是否有读取权限，主要是针对6.0已上系统
        if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            readLocalMedia();
        } else {
            requestPermission(Constants.READ_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }


    /**
     * 设置选中状态
     */
    private void notifyDataCheckedStatus(List<LocalMedia> medias) {
        try {
            // 获取选中图片
            if (medias == null) {
                medias = new ArrayList<>();
            }

            List<LocalMediaFolder> folders = adapter.getFolderData();
            for (LocalMediaFolder folder : folders) {
                // 只重置之前有选中过的文件夹，因为有可能也取消选中的
                if (folder.isChecked()) {
                    folder.setCheckedNum(0);
                    folder.setChecked(false);
                }
            }

            if (medias.size() > 0) {
                for (LocalMediaFolder folder : folders) {
                    int num = 0;// 记录当前相册下有多少张是选中的
                    List<LocalMedia> images = folder.getImages();
                    for (LocalMedia media : images) {
                        String path = media.getPath();
                        for (LocalMedia m : medias) {
                            if (path.equals(m.getPath())) {
                                num++;
                                folder.setChecked(true);
                                folder.setCheckedNum(num);
                            }
                        }
                    }
                }
            }
            adapter.bindFolderData(folders);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void readLocalMedia() {
        /**
         * 根据type决定，查询本地图片或视频。
         */
        new LocalMediaLoader(this, type).loadAllImage(new LocalMediaLoader.LocalMediaLoadListener() {

            @Override
            public void loadComplete(List<LocalMediaFolder> folders) {
                if (folders.size() > 0) {
                    tv_empty.setVisibility(View.GONE);
                    adapter.bindFolderData(folders);
                    notifyDataCheckedStatus(medias);
                } else {
                    tv_empty.setVisibility(View.VISIBLE);
                    switch (type) {
                        case LocalMediaLoader.TYPE_IMAGE:
                            tv_empty.setText(getString(R.string.no_photo));
                            break;
                        case LocalMediaLoader.TYPE_VIDEO:
                            tv_empty.setText(getString(R.string.no_video));
                            break;
                    }

                }
            }
        });
    }


    @Override
    public void onLeftClick() {

    }

    @Override
    public void onRightClick() {
        finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.tv_empty) {
            List<LocalMedia> images = new ArrayList<>();
            startImageGridActivity(titleBar.getTitleText(), images);
        }
    }

    @Override
    public void onItemClick(String folderName, List<LocalMedia> images) {
        if (images != null && images.size() > 0) {
            startImageGridActivity(folderName, images);
        }
    }


    private void startImageGridActivity(String folderName, List<LocalMedia> images) {
        Intent intent = new Intent();
        List<LocalMediaFolder> folders = adapter.getFolderData();
        String toJson = gson.toJson(images);
        saveObject(toJson, Constants.EXTRA_IMAGES);
        intent.putExtra(Constants.EXTRA_PREVIEW_SELECT_LIST, (Serializable) medias);
        intent.putExtra(Constants.FOLDER_NAME, folderName);
        intent.putExtra(Constants.EXTRA_ENABLE_PREVIEW, enablePreview);
        intent.putExtra(Constants.EXTRA_SHOW_CAMERA, showCamera);
        intent.putExtra(Constants.EXTRA_SELECT_MODE, selectMode);
        intent.putExtra(Constants.EXTRA_ENABLE_CROP, enableCrop);
        intent.putExtra(Constants.EXTRA_MAX_SELECT_NUM, maxSelectNum);
        intent.putExtra(Constants.EXTRA_TYPE, type);
        intent.putExtra(Constants.EXTRA_CROP_MODE, copyMode);
        intent.putExtra(Constants.EXTRA_ENABLE_PREVIEW_VIDEO, enablePreviewVideo);
        intent.putExtra(Constants.EXTRA_FOLDERS, (Serializable) folders);
        intent.putExtra(Constants.BACKGROUND_COLOR, backgroundColor);
        intent.putExtra(Constants.CHECKED_DRAWABLE, cb_drawable);
        intent.putExtra(Constants.EXTRA_COMPRESS, isCompress);
        intent.putExtra(Constants.EXTRA_CROP_W, cropW);
        intent.putExtra(Constants.EXTRA_CROP_H, cropH);
        intent.setClass(mContext, ImageGridActivity.class);
        startActivityForResult(intent, Constants.REQUEST_IMAGE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Constants.REQUEST_IMAGE:
                    // 单选图片裁剪完调用
                    int type = data.getIntExtra("type", 0);
                    if (type == 1) {
                        folders = (List<LocalMediaFolder>) data.getSerializableExtra(Constants.EXTRA_FOLDERS);
                        medias = (List<LocalMedia>) data.getSerializableExtra(Constants.EXTRA_PREVIEW_SELECT_LIST);
                        if (folders != null)
                            adapter.bindFolderData(folders);
                        if (medias == null)
                            medias = new ArrayList<>();
                        notifyDataCheckedStatus(medias);
                        if (tv_empty.getVisibility() == View.VISIBLE && adapter.getFolderData().size() > 0)
                            tv_empty.setVisibility(View.GONE);
                    } else {
                        ArrayList<String> images = (ArrayList<String>) data.getSerializableExtra(Constants.REQUEST_OUTPUT);
                        if (images == null)
                            images = new ArrayList<>();
                        setResult(RESULT_OK, new Intent().putStringArrayListExtra(Constants.REQUEST_OUTPUT, images));
                        finish();
                    }
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }

}
