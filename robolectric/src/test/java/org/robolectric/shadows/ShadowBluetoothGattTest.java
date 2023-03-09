package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/** Tests for {@link ShadowBluetoothGatt}. */
@RunWith(AndroidJUnit4.class)
@Config(minSdk = JELLY_BEAN_MR2)
public class ShadowBluetoothGattTest {

  private static final String MOCK_MAC_ADDRESS = "00:11:22:33:AA:BB";
  private static final String ACTION_CONNECTION = "CONNECT/DISCONNECT";
  private static final String ACTION_DISCOVER = "DISCOVER";
  private static final String ACTION_READ = "READ";
  private static final String ACTION_WRITE = "WRITE";
  private static final String REMOTE_ADDRESS = "R-A";

  private int resultStatus = INITIAL_VALUE;
  private int resultState = INITIAL_VALUE;
  private String resultAction;
  private BluetoothGattCharacteristic resultCharacteristic;
  private BluetoothGatt bluetoothGatt;

  private static final BluetoothGattService service1 =
      new BluetoothGattService(
          UUID.fromString("00000000-0000-0000-0000-0000000000A1"),
          BluetoothGattService.SERVICE_TYPE_PRIMARY);
  private static final BluetoothGattService service2 =
      new BluetoothGattService(
          UUID.fromString("00000000-0000-0000-0000-0000000000A2"),
          BluetoothGattService.SERVICE_TYPE_SECONDARY);

  private final BluetoothGattCallback callback =
      new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
          resultStatus = status;
          resultState = newState;
          resultAction = ACTION_CONNECTION;
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
          resultStatus = status;
          resultAction = ACTION_DISCOVER;
        }

        @Override
        public void onCharacteristicRead(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
          resultStatus = status;
          resultCharacteristic = characteristic;
          resultAction = ACTION_READ;
        }

        @Override
        public void onCharacteristicWrite(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
          resultStatus = status;
          resultCharacteristic = characteristic;
          resultAction = ACTION_WRITE;
        }
      };

  private final BluetoothGattCharacteristic characteristicWithReadProperty =
      new BluetoothGattCharacteristic(
          UUID.fromString("00000000-0000-0000-0000-0000000000A3"),
          BluetoothGattCharacteristic.PROPERTY_READ,
          BluetoothGattCharacteristic.PERMISSION_READ);

  private final BluetoothGattCharacteristic characteristicWithWriteProperties =
      new BluetoothGattCharacteristic(
          UUID.fromString("00000000-0000-0000-0000-0000000000A4"),
          BluetoothGattCharacteristic.PROPERTY_WRITE
              | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
          BluetoothGattCharacteristic.PERMISSION_WRITE);

  @Before
  public void setUp() throws Exception {
    BluetoothDevice bluetoothDevice = ShadowBluetoothDevice.newInstance(MOCK_MAC_ADDRESS);
    bluetoothGatt = ShadowBluetoothGatt.newInstance(bluetoothDevice);
  }

  @After
  public void tearDown() {
    shadowOf(bluetoothGatt).getBluetoothConnectionManager().resetConnections();
  }

  @Test
  public void canCreateBluetoothGattViaNewInstance() {
    BluetoothDevice bluetoothDevice = ShadowBluetoothDevice.newInstance(MOCK_MAC_ADDRESS);
    BluetoothGatt bluetoothGatt = ShadowBluetoothGatt.newInstance(bluetoothDevice);
    assertThat(bluetoothGatt).isNotNull();
  }

  @Test
  public void canSetAndGetGattCallback() {
    BluetoothDevice bluetoothDevice = ShadowBluetoothDevice.newInstance(MOCK_MAC_ADDRESS);
    BluetoothGatt bluetoothGatt = ShadowBluetoothGatt.newInstance(bluetoothDevice);
    BluetoothGattCallback callback = new BluetoothGattCallback() {};

    shadowOf(bluetoothGatt).setGattCallback(callback);

    assertThat(shadowOf(bluetoothGatt).getGattCallback()).isEqualTo(callback);
  }

  @Config(minSdk = JELLY_BEAN_MR2)
  public void connect_returnsTrue() {
    BluetoothDevice bluetoothDevice = ShadowBluetoothDevice.newInstance(MOCK_MAC_ADDRESS);
    BluetoothGatt bluetoothGatt = ShadowBluetoothGatt.newInstance(bluetoothDevice);
    assertThat(bluetoothGatt.connect()).isTrue();
  }

  @Test
  public void test_getBluetoothConnectionManager() {
    assertThat(shadowOf(bluetoothGatt).getBluetoothConnectionManager()).isNotNull();
  }

  @Test
  public void test_notifyConnection_connects() {
    shadowOf(bluetoothGatt).notifyConnection(REMOTE_ADDRESS);
    assertThat(shadowOf(bluetoothGatt).isConnected()).isTrue();
    assertThat(
            shadowOf(bluetoothGatt)
                .getBluetoothConnectionManager()
                .hasGattClientConnection(REMOTE_ADDRESS))
        .isTrue();
    assertThat(resultStatus).isEqualTo(INITIAL_VALUE);
    assertThat(resultState).isEqualTo(INITIAL_VALUE);
    assertThat(resultAction).isNull();
  }

  @Test
  public void test_notifyConnection_connectsWithCallbackSet() {
    shadowOf(bluetoothGatt).setGattCallback(callback);
    shadowOf(bluetoothGatt).notifyConnection(REMOTE_ADDRESS);
    assertThat(shadowOf(bluetoothGatt).isConnected()).isTrue();
    assertThat(
            shadowOf(bluetoothGatt)
                .getBluetoothConnectionManager()
                .hasGattClientConnection(REMOTE_ADDRESS))
        .isTrue();
    assertThat(resultStatus).isEqualTo(BluetoothGatt.GATT_SUCCESS);
    assertThat(resultState).isEqualTo(BluetoothProfile.STATE_CONNECTED);
    assertThat(resultAction).isEqualTo(ACTION_CONNECTION);
  }

  @Test
  public void test_notifyDisconnection_disconnects() {
    shadowOf(bluetoothGatt).notifyDisconnection(REMOTE_ADDRESS);
    assertThat(shadowOf(bluetoothGatt).isConnected()).isFalse();
    assertThat(
            shadowOf(bluetoothGatt)
                .getBluetoothConnectionManager()
                .hasGattClientConnection(REMOTE_ADDRESS))
        .isFalse();
    assertThat(resultStatus).isEqualTo(INITIAL_VALUE);
    assertThat(resultState).isEqualTo(INITIAL_VALUE);
    assertThat(resultAction).isNull();
  }

  @Test
  public void test_notifyDisconnection_disconnectsWithCallbackSet() {
    shadowOf(bluetoothGatt).setGattCallback(callback);
    shadowOf(bluetoothGatt).notifyDisconnection(REMOTE_ADDRESS);
    assertThat(shadowOf(bluetoothGatt).isConnected()).isFalse();
    assertThat(
            shadowOf(bluetoothGatt)
                .getBluetoothConnectionManager()
                .hasGattClientConnection(REMOTE_ADDRESS))
        .isFalse();
    assertThat(resultStatus).isEqualTo(INITIAL_VALUE);
    assertThat(resultState).isEqualTo(INITIAL_VALUE);
    assertThat(resultAction).isNull();
  }

  @Test
  public void test_notifyDisconnection_disconnectsWithCallbackSet_connectedInitially() {
    shadowOf(bluetoothGatt).setGattCallback(callback);
    shadowOf(bluetoothGatt).notifyConnection(REMOTE_ADDRESS);
    shadowOf(bluetoothGatt).notifyDisconnection(REMOTE_ADDRESS);
    assertThat(
            shadowOf(bluetoothGatt)
                .getBluetoothConnectionManager()
                .hasGattClientConnection(REMOTE_ADDRESS))
        .isFalse();
    assertThat(shadowOf(bluetoothGatt).isConnected()).isFalse();
    assertThat(resultStatus).isEqualTo(BluetoothGatt.GATT_SUCCESS);
    assertThat(resultState).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    assertThat(resultAction).isEqualTo(ACTION_CONNECTION);
  }
}
