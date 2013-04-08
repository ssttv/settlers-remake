package jsettlers.main.android.resources;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import jsettlers.graphics.progress.EProgressState;
import jsettlers.graphics.progress.ProgressConnector;
import jsettlers.main.android.R;
import jsettlers.main.android.Revision;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

public class ResourceUpdater implements Runnable {

	private static final String RESOURCE_PREFIX = "";
	private static final String SERVER_ROOT =
	        "https://michael2402.homeip.net/jsettlers/";
	private static final String PREF_REVISION = "rev";
	private static final String PREF_OUTDATED = "force";
	private final Resources resources;
	private final File destdir;

	private boolean isUpdating;
	private boolean needsUpdate = false;
	private String serverrev = "";
	private final SharedPreferences prefs;

	public ResourceUpdater(Context context, File destdir) {
		this.resources = context.getResources();
		this.prefs = context.getSharedPreferences("resupdate", 0);
		this.destdir = destdir;

		if (prefs.getInt(PREF_REVISION, -1) != Revision.REVISION
		        || prefs.getBoolean(PREF_OUTDATED, false)) {
			needsUpdate = true;
		}
	}

	@Override
	public void run() {
		try {
			DefaultHttpClient httpClient = createClient();
			String myversion = getMyVersion(getVersionFile());

			if (myversion.isEmpty()) {
				needsUpdate = true;
			}

			serverrev = loadRevision(httpClient);

			boolean serverrevIsNewer =
			        serverrev != null && !serverrev.equals(myversion);
			if (serverrevIsNewer) {
				needsUpdate = true;
				prefs.edit().putBoolean(PREF_OUTDATED, true).commit();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private File getVersionFile() {
		return new File(destdir, "version");
	}

	public void startUpdate(final UpdateListener listener,
	        final ProgressConnector c) {
		if (isUpdating()) {
			// bad. really bad.
			return;
		}
		needsUpdate = false;

		setUpdating(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					updateFiles(createClient(), c);
				} catch (Throwable t) {
					setUpdating(false);
				}
				c.setProgressState(EProgressState.UPDATE, 1);
				if (listener != null) {
					listener.resourceUpdateFinished();
				}

			}
		}, "resource-update").start();
	}

	private void updateFiles(DefaultHttpClient httpClient, ProgressConnector c)
	        throws IOException, ClientProtocolException {
		c.setProgressState(EProgressState.UPDATE, -1);

		if (serverrev == null) {
			serverrev = loadRevision(httpClient);
		}

		final String url = SERVER_ROOT + "resources.zip";
		HttpGet httpRequest = new HttpGet(url);
		HttpResponse response = httpClient.execute(httpRequest);
		ZipInputStream inputStream =
		        new ZipInputStream(response.getEntity().getContent());

		try {

			int files = 0;

			byte[] buffer = new byte[1024];

			int size = inputStream.available();

			ZipEntry entry;
			while ((entry = inputStream.getNextEntry()) != null) {
				String name = entry.getName();
				c.setProgressState(EProgressState.UPDATE,
				        (float) (size - inputStream.available()) / size);

				if (name.startsWith(RESOURCE_PREFIX)) {
					String outfilename =
					        destdir.getAbsolutePath() + "/"
					                + name.substring(RESOURCE_PREFIX.length());
					File outfile = new File(outfilename);
					if (entry.isDirectory()) {
						if (outfile.exists() && !outfile.isDirectory()) {
							outfile.delete();
						}
						if (!outfile.isDirectory()) {
							outfile.mkdirs();
						}
					} else {
						File tmpFile = new File(outfilename + ".tmp");
						tmpFile.getParentFile().mkdirs();
						tmpFile.deleteOnExit(); // <- if something fails
						FileOutputStream out = new FileOutputStream(tmpFile);

						while (true) {
							int len = inputStream.read(buffer);
							if (len <= 0) {
								break;
							}
							out.write(buffer, 0, len);
						}
						out.close();
						tmpFile.renameTo(outfile);
						files++;
					}
				}
			}
			System.out.println("Updated " + files + " files");

			writeMyVersion(getVersionFile(), serverrev);
			prefs.edit().putInt(PREF_REVISION, Revision.REVISION)
			        .putBoolean(PREF_OUTDATED, false).commit();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		setUpdating(false);
	}

	private static String getMyVersion(File versionfile) throws IOException {
		if (versionfile.exists()) {
			return new DataInputStream(new FileInputStream(versionfile))
			        .readUTF();
		} else {
			return "";
		}
	}

	private static void writeMyVersion(File versionfile, String version)
	        throws IOException {
		new DataOutputStream(new FileOutputStream(versionfile))
		        .writeUTF(version);
	}

	private static String loadRevision(DefaultHttpClient httpClient)
	        throws IOException, ClientProtocolException {
		final String url = SERVER_ROOT + "revision.txt";
		HttpGet httpRequest = new HttpGet(url);
		HttpResponse response = httpClient.execute(httpRequest);
		InputStream inputStream = response.getEntity().getContent();
		return getString(inputStream);
	}

	private static String getString(InputStream inputStream) {
		return new Scanner(inputStream).useDelimiter("\\A").next();
	}

	private DefaultHttpClient createClient() throws KeyStoreException,
	        NoSuchAlgorithmException, CertificateException, IOException,
	        KeyManagementException, UnrecoverableKeyException {
		HostnameVerifier hostnameVerifier =
		        SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

		DefaultHttpClient client = new DefaultHttpClient();

		KeyStore truststore = KeyStore.getInstance("BKS");
		InputStream in = resources.openRawResource(R.raw.certs);

		truststore.load(in, "F2rORYtG".toCharArray());

		SSLSocketFactory socketFactory = new SSLSocketFactory(truststore);

		SchemeRegistry registry = new SchemeRegistry();
		socketFactory
		        .setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		registry.register(new Scheme("https", socketFactory, 443));
		SingleClientConnManager mgr =
		        new SingleClientConnManager(client.getParams(), registry);
		DefaultHttpClient httpClient =
		        new DefaultHttpClient(mgr, client.getParams());

		// Set verifier
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
		return httpClient;
	}

	public synchronized boolean isUpdating() {
		return isUpdating;
	}

	public synchronized void waitUntilUpdateFinished()
	        throws InterruptedException {
		while (isUpdating) {
			this.wait();
		}
	}

	private synchronized void setUpdating(boolean isUpdating) {
		this.isUpdating = isUpdating;
		this.notifyAll();
	}

	public boolean needsUpdate() {
		return needsUpdate;
	}
}
