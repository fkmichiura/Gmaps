package com.example.fkmichiura.gmaps.views;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.fkmichiura.gmaps.controllers.InfoWindowAdapter;
import com.example.fkmichiura.gmaps.R;
import com.example.fkmichiura.gmaps.models.HealthcareCenter;
import com.example.fkmichiura.gmaps.models.Scheduling;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, RoutingListener {

    private GoogleMap mMap;
    private double currentLat, currentLong;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference dataRef;

    private ArrayList<HealthcareCenter> healthcareCenters = new ArrayList<>();
    private ArrayList<Scheduling> schedulings = new ArrayList<>();
    private ArrayList<Float> distances = new ArrayList<>();
    private List<Polyline> polylines = new ArrayList<>();

    private static final int[] COLORS = new int[]{R.color.colorPrimary};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.maps_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_nearby:
                getRouteToDestination();
                break;

            case R.id.menu_logout:
                userSignOut();
                break;
        }
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //InfoWindow Adapter
        InfoWindowAdapter adapter = new InfoWindowAdapter(this);
        mMap.setInfoWindowAdapter(adapter);
        mMap.setOnInfoWindowClickListener(this);

        enableMapControls();

        //Move a câmera para uma posição inicial do mapa
        LatLng location = new LatLng(-22.123054, -51.388255);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 14.0f));//Move the camera to the user's location and zoom in!

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        dataRef = mFirebaseDatabase.getReference();
        dataRef.child("ubs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getFirebaseData(dataSnapshot);
                showMarkers(mMap);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Evento de clique na InfoWindow dos marcadores
    @Override
    public void onInfoWindowClick(final Marker marker) {
        final String index = marker.getId().replace("m", "");
        getSchedulingData(index);

        TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                String schedulingTime = i + ":" + i1;

                try {
                    if (checkSchedulingTime(schedulings, schedulingTime) && checkBeforeTime(schedulingTime)) {
                        sendScheduling(schedulingTime, index);
                    } else {
                        Toast.makeText(
                                MapsActivity.this,
                                "Você não pode agendar a consulta neste horário",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };
        int mHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int mMinute = Calendar.getInstance().get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, timeSetListener, mHour, mMinute, true);
        timePickerDialog.setTitle("Selecione o horário");
        timePickerDialog.show();
    }

    //Recupera os dados da Firebase, adiciona na ArrayList e mostra
    //os marcadores dos respectivos locais na ArrayList
    private void getFirebaseData(DataSnapshot dataSnapshot) {

        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            HealthcareCenter e = ds.getValue(HealthcareCenter.class);
            healthcareCenters.add(e);
        }
    }

    //Atribui os dados recuperados na Firebase aos Marcadores no Mapa
    private void showMarkers(GoogleMap googleMap) {
        for (HealthcareCenter e : healthcareCenters) {
            // Adiciona um marcador no local
            LatLng location = new LatLng(e.getLatitude(), e.getLongitude());
            MarkerOptions options = new MarkerOptions()
                    .position(location)
                    .title(e.getName())
                    .snippet(e.getAddress());
            googleMap.addMarker(options);
        }
    }

    //Mostra o botão de capturar posição atual e realiza tal operação
    private void enableMapControls() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    //Encerra sessão do usuário e volta pra Activity de Login
    private void userSignOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    private void getSchedulingData(String index) {
        //Flush no ArrayList antes de iniciar as operações
        schedulings.clear();

        dataRef = mFirebaseDatabase.getReference();
        dataRef.child("ubs/" + index + "/scheduling").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Scheduling scheduling = ds.getValue(Scheduling.class);
                    schedulings.add(scheduling);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapsActivity.this, "Erro: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    //---------------------Rota ---------------------
    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null){
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, "Houve um erro inesperado. Tente novamente", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int index) {
        drawRoute(route);
    }

    @Override
    public void onRoutingCancelled() {

    }

    //Envia os dados relativos ao agendamento à Firebase
    public void sendScheduling(String time, String index) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // User is signed in
            Scheduling scheduling = new Scheduling(user.getUid(), user.getEmail(),
                    getCurrentDate(), time);

            //Cria instância do Firebase Database
            String key = dataRef.push().getKey();
            dataRef = mFirebaseDatabase.getReference("ubs/" + index + "/scheduling").child(key);
            dataRef.setValue(scheduling);
        } else {
            // No user is signed in
            Toast.makeText(MapsActivity.this, "O usuário não encontra-se logado", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }

    //Captura a data atual
    public String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdformat = new SimpleDateFormat("dd/MM/yyyy");
        sdformat.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        return sdformat.format(calendar.getTime());
    }

    //Captura a hora atual
    public String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdformat = new SimpleDateFormat("HH:mm");
        sdformat.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        return sdformat.format(calendar.getTime());
    }

    //Verifica se o horário selecionado já não consta na lista de agendamento
    public boolean checkSchedulingTime(ArrayList<Scheduling> scheduleList, String currentTime) {
        for (Scheduling s : scheduleList) {
            if (s.getTime().equals(currentTime)) {
                return false;
            }
        }
        return true;
    }

    //Verifica se o horário selecionado pelo usuário se encontra dentro do intervalo de funcionamento
    //da UBS e se o horário é posterior ao horário atual
    public boolean checkBeforeTime(String time) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date openTime = sdf.parse("07:00");
        Date closeTime = sdf.parse("16:30");
        Date choosenTime = sdf.parse(time);
        Date currentTime = sdf.parse(getCurrentTime());

        return (choosenTime.after(openTime) && choosenTime.before(closeTime) && choosenTime.after(currentTime));
    }

    //Calcula a distância da localização atual com todos os marcadores de UBS e verifica o de menor distância
    //-------------------------------------------------------------------------------------------------------
    public HealthcareCenter getNearestHealthcareCenter(){
        distances.clear();

        if(mMap != null){
            currentLat = mMap.getMyLocation().getLatitude();
            currentLong = mMap.getMyLocation().getLongitude();
        }
        for (HealthcareCenter h : healthcareCenters){
            float[] results = new float[1];
            Location.distanceBetween(currentLat, currentLong, h.getLatitude(), h.getLongitude(), results);
            distances.add(results[0]);
        }
        float distMin = Collections.min(distances);
        return (healthcareCenters.get(distances.indexOf(distMin)));
    }

    //Configuração da captura dos dados e armazenamento das linhas referentes a rota
    public void getRouteToDestination(){
        deleteRoutes();
        HealthcareCenter healthcareCenter = getNearestHealthcareCenter();

        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(currentLat, currentLong), new LatLng(healthcareCenter.getLatitude(), healthcareCenter.getLongitude()))
                .key("AIzaSyCGQTArch3CECItxevhMGebfkWiWNvxheg")
                .build();
        routing.execute();
    }

    //Remove todos os itens referentes as linhas da rota
    public void deleteRoutes(){
        for(Polyline p : polylines){
            p.remove();
        }
        polylines.clear();
    }

    //Desenha as linhas da rota, dados os passos armazenados
    public void drawRoute(ArrayList<Route> route){
        if(polylines.size() > 0) {
            deleteRoutes();
        }
        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);
        }
    }
}