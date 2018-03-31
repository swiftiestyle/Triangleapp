package com.mocircle.cidrawingsample;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.mocircle.cidrawing.ConfigManager;
import com.mocircle.cidrawing.DrawingBoard;
import com.mocircle.cidrawing.DrawingBoardManager;
import com.mocircle.cidrawing.board.Layer;
import com.mocircle.cidrawing.board.LayerManager;
import com.mocircle.cidrawing.element.DrawElement;
import com.mocircle.cidrawing.element.PhotoElement;
import com.mocircle.cidrawing.element.TextElement;
import com.mocircle.cidrawing.element.shape.ArcElement;
import com.mocircle.cidrawing.element.shape.CircleElement;
import com.mocircle.cidrawing.element.shape.IsoscelesTriangleElement;
import com.mocircle.cidrawing.element.shape.LineElement;
import com.mocircle.cidrawing.element.shape.OvalElement;
import com.mocircle.cidrawing.element.shape.RectElement;
import com.mocircle.cidrawing.element.shape.RightTriangleElement;
import com.mocircle.cidrawing.element.shape.SquareElement;
import com.mocircle.cidrawing.mode.DrawingMode;
import com.mocircle.cidrawing.mode.InsertPhotoMode;
import com.mocircle.cidrawing.mode.InsertShapeMode;
import com.mocircle.cidrawing.mode.InsertTextMode;
import com.mocircle.cidrawing.mode.PointerMode;
import com.mocircle.cidrawing.mode.eraser.ObjectEraserMode;
import com.mocircle.cidrawing.mode.selection.LassoSelectionMode;
import com.mocircle.cidrawing.mode.selection.OvalSelectionMode;
import com.mocircle.cidrawing.mode.selection.RectSelectionMode;
import com.mocircle.cidrawing.mode.stroke.EraserStrokeMode;
import com.mocircle.cidrawing.mode.stroke.PlainStrokeMode;
import com.mocircle.cidrawing.mode.stroke.SmoothStrokeMode;
import com.mocircle.cidrawing.mode.transformation.MoveMode;
import com.mocircle.cidrawing.mode.transformation.ResizeMode;
import com.mocircle.cidrawing.mode.transformation.RotateMode;
import com.mocircle.cidrawing.mode.transformation.SkewMode;
import com.mocircle.cidrawing.operation.AlignmentOperation;
import com.mocircle.cidrawing.operation.ArrangeOperation;
import com.mocircle.cidrawing.operation.FlipOperation;
import com.mocircle.cidrawing.operation.GroupElementOperation;
import com.mocircle.cidrawing.operation.PathOperation;
import com.mocircle.cidrawing.operation.ReshapeOperation;
import com.mocircle.cidrawing.operation.UngroupElementOperation;
import com.mocircle.cidrawing.persistence.ExportData;
import com.mocircle.cidrawing.view.CiDrawingView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CiDrawingView drawingView;
    private DrawerLayout drawer;
    private RecyclerView layersView;

    private DrawingBoard drawingBoard;
    private LayerAdapter layerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupView();
        setupLayerView();

        drawingBoard = DrawingBoardManager.getInstance().createDrawingBoard();
        setupDrawingBoard();
        drawingBoard.getElementManager().createNewLayer();
        drawingBoard.getElementManager().selectFirstVisibleLayer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    private void setupView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setScrimColor(Color.TRANSPARENT);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupDrawingBoard() {
        drawingView = (CiDrawingView) findViewById(R.id.drawing_view);
        drawingBoard.setupDrawingView(drawingView);
        drawingBoard.getDrawingContext().getPaint().setColor(Color.BLACK);
        drawingBoard.getDrawingContext().getPaint().setStrokeWidth(6);
        drawingBoard.getDrawingContext().setDrawingMode(new PointerMode());

        layerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                drawingBoard.getDrawingView().notifyViewUpdated();
            }
        });
        layerAdapter.setOnItemClick(new LayerAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, Layer layer) {
                drawingBoard.getElementManager().selectLayer(layer);
                layerAdapter.notifyDataSetChanged();
            }
        });
        drawingBoard.getElementManager().addLayerChangeListener(new LayerManager.LayerChangeListener() {
            @Override
            public void onLayerChanged() {
                layerAdapter.setLayers(Arrays.asList(drawingBoard.getElementManager().getLayers()));
            }
        });
    }

    private void setupLayerView() {
        layersView = (RecyclerView) findViewById(R.id.layers_view);
        layersView.setLayoutManager(new LinearLayoutManager(this));
        layerAdapter = new LayerAdapter();
        layersView.setAdapter(layerAdapter);
    }
    public void transform(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_transform, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DrawingMode mode = null;
                switch (item.getItemId()) {

                    case R.id.resize_menu:
                        mode = new ResizeMode(true);
                        break;

                }
                drawingBoard.getDrawingContext().setDrawingMode(mode);
                return true;
            }
        });
        popup.show();
    }

    public void insertShape(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_insert_shape, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                InsertShapeMode mode = new InsertShapeMode();
                drawingBoard.getDrawingContext().setDrawingMode(mode);
                switch (item.getItemId()) {

                    case R.id.isosceles_triangle_menu:
                        mode.setShapeType(IsoscelesTriangleElement.class);
                        break;
                    case R.id.right_triangle_menu:
                        RightTriangleElement shape = new RightTriangleElement();
                        shape.setLeftRightAngle(true);
                        mode.setShapeInstance(shape);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }


    public void reshape(View v) {
        drawingBoard.getOperationManager().executeOperation(new ReshapeOperation());
    }



    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void pathUnion(View v) {
        PathOperation operation = new PathOperation();
        operation.setPathOp(Path.Op.UNION);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void pathIntersect(View v) {
        PathOperation operation = new PathOperation();
        operation.setPathOp(Path.Op.INTERSECT);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void pathDifferent(View v) {
        PathOperation operation = new PathOperation();
        operation.setPathOp(Path.Op.DIFFERENCE);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void pathXor(View v) {
        PathOperation operation = new PathOperation();
        operation.setPathOp(Path.Op.XOR);
        drawingBoard.getOperationManager().executeOperation(operation);
    }




    public void switchDrawingType() {
        if (drawingBoard.getConfigManager().getDrawingType() == ConfigManager.DrawingType.Vector) {
            drawingBoard.getConfigManager().setDrawingType(ConfigManager.DrawingType.Painting);
            Toast.makeText(this, "Switch to Painting type.", Toast.LENGTH_SHORT).show();
        } else {
            drawingBoard.getConfigManager().setDrawingType(ConfigManager.DrawingType.Vector);
            Toast.makeText(this, "Switch to Vector type.", Toast.LENGTH_SHORT).show();
        }
    }

    public void switchDebugMode() {
        drawingBoard.getConfigManager().setDebugMode(!drawingBoard.getConfigManager().isDebugMode());
        drawingView.notifyViewUpdated();
        Toast.makeText(this, "Debug mode=" + drawingBoard.getConfigManager().isDebugMode() + ".", Toast.LENGTH_SHORT).show();
    }

    public void showInfo() {
        DrawElement element = drawingBoard.getElementManager().getSelection().getSingleElement();
        if (element != null) {
            String msg = "Type: " + element.getClass().getSimpleName();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public void saveDrawing() {
        ExportData data = drawingBoard.exportData();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("drawings", data.getMetaData().toString());
        editor.commit();

        Map<String, byte[]> resMap = data.getResources();
        for (String key : resMap.keySet()) {
            File file = new File(this.getExternalCacheDir(), key);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(resMap.get(key));
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.i(TAG, "Save drawing as: " + data.getMetaData().toString());
        Toast.makeText(this, "Drawing saved.", Toast.LENGTH_SHORT).show();
    }

    public void loadDrawing() {
        String drawings = PreferenceManager.getDefaultSharedPreferences(this).getString("drawings", "");
        Map<String, byte[]> resources = new HashMap<>();
        try {
            File[] files = this.getExternalCacheDir().listFiles();
            for (File file : files) {
                try {
                    RandomAccessFile fis = new RandomAccessFile(file, "r");
                    byte[] bs = new byte[(int) fis.length()];
                    fis.readFully(bs);
                    fis.close();

                    resources.put(file.getName(), bs);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            JSONObject obj = new JSONObject(drawings);
            drawingBoard = DrawingBoardManager.getInstance().createDrawingBoard(obj);
            setupDrawingBoard();
            drawingBoard.importData(obj, resources);
            drawingBoard.getElementManager().selectFirstVisibleLayer();
            Toast.makeText(this, "Drawing loaded.", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(this, "Load drawing failed.", Toast.LENGTH_SHORT).show();
        }
    }
}
