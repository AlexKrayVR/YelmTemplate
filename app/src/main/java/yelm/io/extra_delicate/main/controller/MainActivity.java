package yelm.io.extra_delicate.main.controller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import yelm.io.extra_delicate.basket.controller.BasketActivity;
import yelm.io.extra_delicate.item.controller.ItemFromNotificationActivity;
import yelm.io.extra_delicate.loader.app_settings.SharedPreferencesSetting;
import yelm.io.extra_delicate.main.adapter.MainItemsAdapter;
import yelm.io.extra_delicate.main.categories.CategoriesAdapter;
import yelm.io.extra_delicate.main.categories.CategoriesPOJO;
import yelm.io.extra_delicate.main.news.News;
import yelm.io.extra_delicate.main.news.NewsFromNotificationActivity;
import yelm.io.extra_delicate.rest.query.RestMethods;
import yelm.io.extra_delicate.support_stuff.Logging;
import yelm.io.extra_delicate.search.SearchActivity;
import yelm.io.extra_delicate.database.basket_new.BasketCart;
import yelm.io.extra_delicate.database.user_addresses.UserAddress;
import yelm.io.extra_delicate.databinding.ActivityMainBinding;
import yelm.io.extra_delicate.main.model.CategoriesWithProductsClass;
import yelm.io.extra_delicate.main.model.Modifier;
import yelm.io.extra_delicate.rest.rest_api.RestAPI;
import yelm.io.extra_delicate.rest.client.RetrofitClient;
import yelm.io.extra_delicate.constants.Constants;
import yelm.io.extra_delicate.support_stuff.StaticRepository;
import yelm.io.extra_delicate.user_address.controller.AddressesBottomSheet;
import yelm.io.extra_delicate.chat.controller.ChatActivity;
import yelm.io.extra_delicate.R;
import yelm.io.extra_delicate.database.Common;
import yelm.io.extra_delicate.main.news.NewsAdapter;
import yelm.io.extra_delicate.support_stuff.ItemOffsetDecorationRight;
import yelm.io.extra_delicate.user_account.LoginHostActivity;

public class MainActivity extends AppCompatActivity implements AddressesBottomSheet.AddressesBottomSheetListener {

    ActivityMainBinding binding;
    AddressesBottomSheet addressesBottomSheet = new AddressesBottomSheet();

    private ArrayList<CategoriesWithProductsClass> catalogsWithProductsList = new ArrayList<>();

    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 7777;

    private final CompositeDisposable compositeDisposableBasket = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding();
        setCustomColor();
        getCategoriesWithProducts("0", "0");
        initNews();
        getAppToken();
        getCategories();
        getLocationPermission();
        Bundle args = getIntent().getExtras();
        if (args != null) {
            Logging.logDebug("MainActivity - Notification data: " + args.toString());
            if (Objects.equals(args.getString("name"), "news")) {
                Intent intent = new Intent(MainActivity.this, NewsFromNotificationActivity.class);
                intent.putExtra("id", args.getString("id"));
                startActivity(intent);
            } else if (Objects.equals(args.getString("name"), "item")) {
                Intent intent = new Intent(MainActivity.this, ItemFromNotificationActivity.class);
                intent.putExtra("id", args.getString("id"));
                startActivity(intent);
            } else if (Objects.equals(args.getString("name"), "chat")) {
                startActivity(new Intent(MainActivity.this, ChatActivity.class));
            }
        }
    }

    /**
     * set user chosen colors
     */
    private void setCustomColor() {
        binding.addressLayout.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.categoryExpand.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.basket.getBackground().setTint(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_COLOR)));
        binding.userCurrentAddress.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        binding.basket.setTextColor(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
        for (Drawable drawable : binding.basket.getCompoundDrawablesRelative()) {
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)), PorterDuff.Mode.SRC_IN));
            }
        }
        binding.progress.getIndeterminateDrawable().setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)), PorterDuff.Mode.SRC_IN);
        binding.categoryExpand.setColorFilter(Color.parseColor("#" + SharedPreferencesSetting.getDataString(SharedPreferencesSetting.APP_TEXT_COLOR)));
    }

    private void checkIfGPSEnabled() {
        if (!StaticRepository.isLocationEnabled(this)) {
            Snackbar snackbar = Snackbar.make(
                    findViewById(R.id.layout),
                    R.string.mainActivityNoGPS,
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("????????????????", view -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))).setActionTextColor(getResources().getColor(R.color.mainThemeColor));
            snackbar.show();
        }
    }

    private void binding() {
        binding.chat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        binding.recyclerCards.setHasFixedSize(false);
        binding.recyclerCards.addItemDecoration(new ItemOffsetDecorationRight((int) getResources().getDimension(R.dimen.dimens_16dp)));
        binding.addressLayout.setOnClickListener(v -> callAddressesBottomSheet());
        binding.userCurrentAddress.setOnClickListener(v -> callAddressesBottomSheet());
        binding.search.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        binding.basket.setOnClickListener(v -> startActivity(new Intent(this, BasketActivity.class)));
        binding.categoryExpand.setOnClickListener(v -> {
            if (binding.recyclerCategories.getVisibility() == View.VISIBLE) {
                binding.recyclerCategories.setVisibility(View.GONE);
                binding.categoryExpand.setRotation(0);
            } else {
                binding.recyclerCategories.setVisibility(View.VISIBLE);
                binding.categoryExpand.setRotation(90);
            }
        });

        binding.userLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginHostActivity.class)));

    }

    private void callAddressesBottomSheet() {
        //check if AddressesBottomSheet is added otherwise we get exception:
        //java.lang.IllegalStateException: Fragment already added
        if (!addressesBottomSheet.isAdded()) {
            addressesBottomSheet.show(getSupportFragmentManager(), "addressBottomSheet");
        }
    }

    private void getLocationPermission() {
        if (hasLocationPermission()) {
            Logging.logDebug("Method getLocationPermission() - Location permission granted");
            getUserCurrentLocation();
        } else {
            Logging.logDebug("Method getLocationPermission() - Location permission not granted");
            ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasLocationPermission() {
        int result = ContextCompat
                .checkSelfPermission(this.getApplicationContext(), LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (hasLocationPermission()) {
                Logging.logDebug("Method onRequestPermissionsResult() - Request Permissions Result: Success!");
                getUserCurrentLocation();
            } else if (shouldShowRequestPermissionRationale(permissions[0])) {
                showDialogExplanationAboutRequestLocationPermission(getText(R.string.mainActivityRequestPermission).toString());
            } else {
                Logging.logDebug("Method onRequestPermissionsResult() - Request Permissions Result: Failed!");
                performIfNoLocationPermission();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions,
                    grantResults);
        }
    }

    private void performIfNoLocationPermission() {
        Logging.logDebug("Method performIfNoLocationPermission()");
        binding.progress.setVisibility(View.GONE);
        if (Common.userAddressesRepository.getUserAddressesList() != null && Common.userAddressesRepository.getUserAddressesList().size() != 0) {
            for (UserAddress userAddress : Common.userAddressesRepository.getUserAddressesList()) {
                if (userAddress.isChecked) {
                    binding.userCurrentAddress.setText(userAddress.address);
                    getCategoriesWithProducts(userAddress.latitude, userAddress.longitude);
                    break;
                }
            }
        } else {
            binding.userCurrentAddress.setText(getText(R.string.choose_address));
        }
    }

    private void showDialogExplanationAboutRequestLocationPermission(String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setTitle(getText(R.string.mainActivityAttention))
                .setOnCancelListener(dialogInterface -> ActivityCompat.requestPermissions(MainActivity.this, LOCATION_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE))
                .setPositiveButton(getText(R.string.mainActivityOk), (dialogInterface, i) -> ActivityCompat.requestPermissions(MainActivity.this, LOCATION_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE))
                .create()
                .show();
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        return super.shouldShowRequestPermissionRationale(permission);
    }

    private void getUserCurrentLocation() {
        Logging.logDebug("Method getUserCurrentLocation()");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(100); //interval in which we want to get location
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setFastestInterval(100);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            performIfNoLocationPermission();
        } else {
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NotNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            binding.progress.setVisibility(View.GONE);
            double latitude = locationResult.getLastLocation().getLatitude();
            double longitude = locationResult.getLastLocation().getLongitude();
            Logging.logDebug("location updated:" + "\nlatitude: " + latitude + "\nlongitude: " + longitude);
            String userStreet = getUserStreet(locationResult.getLastLocation());
            Logging.logDebug("Method getUserCurrentLocation() - userStreet: " + userStreet);
            LocationServices.getFusedLocationProviderClient(MainActivity.this).removeLocationUpdates(locationCallback);

            if (userStreet.trim().isEmpty()) {
                performIfNoLocationPermission();
                return;
            }

            for (UserAddress userAddress : Common.userAddressesRepository.getUserAddressesList()) {
                if (userAddress.isChecked) {
                    userAddress.isChecked = false;
                    Common.userAddressesRepository.updateUserAddresses(userAddress);
                    break;
                }
            }
            UserAddress currentUserAddress = Common.userAddressesRepository.getUserAddressByName(userStreet);
            if (currentUserAddress == null) {
                UserAddress userAddress = new UserAddress(
                        String.valueOf(latitude),
                        String.valueOf(longitude),
                        userStreet, true);
                Common.userAddressesRepository.insertToUserAddresses(userAddress);
                binding.userCurrentAddress.setText(userAddress.address);
            } else {
                currentUserAddress.isChecked = true;
                Common.userAddressesRepository.updateUserAddresses(currentUserAddress);
                binding.userCurrentAddress.setText(currentUserAddress.address);
            }
            getCategoriesWithProducts(String.valueOf(latitude),
                    String.valueOf(longitude));
        }
    };

    private String getUserStreet(Location location) {
        String userStreet = "";
        try {
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1);
            if (addresses.size() > 0) {
                Address userCurrentAddress = addresses.get(0);
                userStreet =
                        (userCurrentAddress.getThoroughfare() == null ? "" : userCurrentAddress.getThoroughfare())
                                + (userCurrentAddress.getThoroughfare() != null && userCurrentAddress.getFeatureName() != null ? ", " : "")
                                + (userCurrentAddress.getFeatureName() == null ? "" : userCurrentAddress.getFeatureName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userStreet;
    }

    private void getCategoriesWithProducts(String Lat, String Lon) {
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                getCategoriesWithChosenProducts(
                        Constants.VERSION,
                        getResources().getConfiguration().locale.getLanguage(),
                        getResources().getConfiguration().locale.getCountry(),
                        RestAPI.PLATFORM_NUMBER,
                        Lat,
                        Lon).
                enqueue(new Callback<ArrayList<CategoriesWithProductsClass>>() {
                    @Override
                    public void onResponse(@NotNull Call<ArrayList<CategoriesWithProductsClass>> call, @NotNull final Response<ArrayList<CategoriesWithProductsClass>> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                catalogsWithProductsList = response.body();
                                if (catalogsWithProductsList.size() != 0) {
                                    Constants.ShopID = catalogsWithProductsList.get(0).getShopID();
                                }
                                Logging.logDebug("Method getCategoriesWithProducts() - Constants.ShopID: " + Constants.ShopID);
                                redrawProducts();
                            } else {
                                Logging.logError("Method getCategoriesWithProducts() - by some reason response is null!");
                            }
                        } else {
                            Logging.logError("Method getCategoriesWithProducts() - response is not successful." +
                                    "Code: " + response.code() + "Message: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<ArrayList<CategoriesWithProductsClass>> call, @NotNull Throwable t) {
                        Logging.logError("Method getCategoriesWithProducts() - failure: " + t.toString());
                    }
                });
    }

    private void getCategories() {
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                getCategories(
                        RestAPI.PLATFORM_NUMBER,
                        getResources().getConfiguration().locale.getLanguage(),
                        getResources().getConfiguration().locale.getCountry()
                ).
                enqueue(new Callback<ArrayList<CategoriesPOJO>>() {
                    @Override
                    public void onResponse(@NotNull Call<ArrayList<CategoriesPOJO>> call, @NotNull final Response<ArrayList<CategoriesPOJO>> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                binding.recyclerCategories.setAdapter(new CategoriesAdapter(MainActivity.this, response.body()));
                            } else {
                                Logging.logError("Method getCategories() - by some reason response is null!");
                            }
                        } else {
                            Logging.logError("Method getCategories() - response is not successful." +
                                    "Code: " + response.code() + "Message: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<ArrayList<CategoriesPOJO>> call, @NotNull Throwable t) {
                        Logging.logError("Method getCategories() - failure: " + t.toString());
                    }
                });
    }

    private void getAppToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Logging.logError("Fetching FCM registration token failed" + task.getException());
                        return;
                    }
                    String token = task.getResult();
                    RestMethods.sendRegistrationToServer(token);
                });
    }

    @Override
    public void selectedAddress(UserAddress selectedUserAddress) {
        binding.progress.setVisibility(View.GONE);
        Logging.logDebug("Method selectedAddress() - address: " + selectedUserAddress.address);
        binding.userCurrentAddress.setText(selectedUserAddress.address);
        getCategoriesWithProducts(selectedUserAddress.latitude, selectedUserAddress.longitude);
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
    }

    private void initNews() {
        RetrofitClient.
                getClient(RestAPI.URL_API_MAIN).
                create(RestAPI.class).
                getNews(Constants.VERSION,
                        getResources().getConfiguration().locale.getLanguage(),
                        getResources().getConfiguration().locale.getCountry(),
                        RestAPI.PLATFORM_NUMBER).
                enqueue(new Callback<ArrayList<News>>() {
                    @Override
                    public void onResponse(@NotNull Call<ArrayList<News>> call, @NotNull final Response<ArrayList<News>> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                if (response.body().size() > 0) {
                                    binding.newsTitle.setVisibility(View.VISIBLE);
                                }
                                binding.recyclerCards.setAdapter(new NewsAdapter(MainActivity.this, response.body()));
                            } else {
                                Logging.logError("Method initNews() - by some reason response is null!");
                            }
                        } else {
                            Logging.logError("Method initNews() - response is not successful." +
                                    "Code: " + response.code() + "Message: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<ArrayList<News>> call, @NotNull Throwable t) {
                        Logging.logError("Method initNews() - failure: " + t.toString());
                    }
                });
    }

    @Override
    public void onStop() {
        compositeDisposableBasket.clear();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfGPSEnabled();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateCost();
        redrawProducts();
    }

    private void updateCost() {
        compositeDisposableBasket.add(Common.basketCartRepository.getBasketCarts()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(carts -> {
                    if (carts.size() == 0) {
                        binding.basket.setVisibility(View.GONE);
                        binding.basket.setText(String.format("0 %s", SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
                        binding.footer.setVisibility(View.GONE);
                    } else {
                        binding.basket.setVisibility(View.VISIBLE);
                        binding.footer.setVisibility(View.VISIBLE);
                        BigDecimal basketPrice = new BigDecimal("0");
                        for (BasketCart cart : carts) {
                            BigDecimal costCurrentCart = new BigDecimal(cart.finalPrice);
                            for (Modifier modifier : cart.modifier) {
                                costCurrentCart = costCurrentCart.add(new BigDecimal(modifier.getValue()));
                            }
                            costCurrentCart = costCurrentCart.multiply(new BigDecimal(cart.count));
                            basketPrice = basketPrice.add(costCurrentCart);
                        }

                        binding.basket.setText(String.format("%s %s", basketPrice.toString(),
                                SharedPreferencesSetting.getDataString(SharedPreferencesSetting.PRICE_IN)));
                        Logging.logDebug("Method updateCost() - carts.size(): " + carts.size() + "\n" +
                                "basketPrice.toString(): " + basketPrice.toString());
                    }
                }));
    }

    synchronized private void redrawProducts() {
        binding.recyclerMainItems.setAdapter(new MainItemsAdapter(this, catalogsWithProductsList));
        Logging.logDebug("Method redrawProducts()");
    }
}