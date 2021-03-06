package edu.purdue.eaps.weatherpipe.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import edu.purdue.eaps.weatherpipe.AWSAnonInterface;
import edu.purdue.eaps.weatherpipe.AWSInterface;
import edu.purdue.eaps.weatherpipe.RadarFilePicker;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import junit.framework.TestCase;

public class AWSInterfaceTest extends TestCase {
	@Test
	public void testListBucket() {
		final String dataBucket = "noaa-nexrad-level2";
		String[] key = {"2010/01/01", "2010/07/14"};
		String jobID = null; 
		AWSInterface awsInterface = new AWSInterface(jobID, null); 
		AWSAnonInterface awsAnonInterface = new AWSAnonInterface();
		List<S3ObjectSummary> summaries; 
		int[] output = new int[2];
		for(int i = 0 ; i < 2; i++) {
			summaries = awsAnonInterface.ListBucket(dataBucket, key[i]);
			output[i] = summaries.size();
			summaries.clear();
		}
		int[] answer = {14104, 33468};
		assertArrayEquals(answer, output);
		System.out.println("ListBucket() is ok");
	}
	
	@Test
	public void testFindOrCreateWeatherPipeJobBucket() {
		String jobID = null; 
		String bucketName = "fdgfhfdx2";
		AWSInterface awsInterface = new AWSInterface(jobID, null);
		jobID = "job1";
		awsInterface = new AWSInterface(jobID, bucketName);
		String output;
		output = awsInterface.FindOrCreateWeatherPipeJobDirectory();
		String answer = "s3n://fdgfhfdx2/";
		assertEquals(answer, output);
		AWSCredentials credentials = new ProfileCredentialsProvider("default").getCredentials();
		// TODO: add better credential searching later
			
		Region region = Region.getRegion(Regions.US_EAST_1);
		AmazonS3Client s3client = new AmazonS3Client(credentials);
		s3client.setRegion(region);
		s3client.deleteBucket(bucketName);
		System.out.println("FindOrCreateWeatherPipeJobBucket() is ok");
	}
	
	@Test
	public void testUploadInputFileList() {
		//ArrayList<String> fileList = new ArrayList<String>();
		ArrayList<ArrayList<String>> fileList1 = new ArrayList<ArrayList<String>>();
		
		//DateTime startTimes = new DateTime(2010, 01, 01, 07, 39, 28);
		//DateTime endTimes = new DateTime(2010, 01, 01, 15, 58, 47);
		DateTime[] startTimes = {new DateTime(2010, 01, 01, 07, 39, 28), new DateTime(2010, 01, 01, 07, 39, 28)};
		DateTime[] endTimes = {new DateTime(2010, 01, 01, 15, 58, 47), new DateTime(2010, 01, 01, 07, 39, 30)};
		String key = "2010/01/01";
		String station = "KBBX";
		final String dataBucket = "noaa-nexrad-level2";
		String[] jobID = {"job1", "job2"};
		//jobID = "job1";
		String[] bucketName = {"fdafda2", "adfeth2"};
		AWSAnonInterface awsAnonInterface = new AWSAnonInterface();
		AWSInterface[] awsInterface = {new AWSInterface(jobID[0], bucketName[0]), new AWSInterface(jobID[1], bucketName[1])};
		//awsInterface.FindOrCreateWeatherPipeJobBucket();
		//AWSInterface awsInterface = new AWSInterface(jobID);
		String[] ret = new String[2];
		for(int i = 0; i < 2; i++) {
			awsInterface[i].FindOrCreateWeatherPipeJobDirectory();
			fileList1.add(RadarFilePicker.getRadarFilesFromTimeRange(startTimes[i], endTimes[i], station, awsAnonInterface, dataBucket));
			ret[i] = awsInterface[i].UploadInputFileList(fileList1.get(i), dataBucket);
		}
		
		
		String[] answer = {"s3n://fdafda2/job1_input", "s3n://adfeth2/job2_input"};
		assertArrayEquals(answer, ret);
		System.out.println("UploadInputFileList() is ok");
		
		
	}
	@Test
	public void testUploadMPJarFile() throws IOException {
		byte[] dataToWrite = {1,2,3,4,5};
		FileOutputStream out = new FileOutputStream("WeatherPipeMapreduce.jar");
		out.write(dataToWrite);
		out.close();
		
		MessageDigest md = null;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm");
		String isoDate = df.format(new Date());
		String jobID = isoDate + "." + Calendar.getInstance().get(Calendar.MILLISECOND);
		AWSInterface awsInterface = new AWSInterface(jobID, null);
		AWSCredentials credentials = new ProfileCredentialsProvider("default").getCredentials();
		String userID = new AmazonIdentityManagementClient(credentials).getUser().getUser().getUserId();
		try {
			md = MessageDigest.getInstance("SHA-256");
			md.update(userID.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] shaHash = md.digest();

		StringBuffer hexSha = new StringBuffer();
		for (byte b : shaHash) {
			hexSha.append(String.format("%02X", b));
		}
		String jobBucketName = "weatherpipe." + hexSha;
		
		awsInterface.addJobBucketName(jobBucketName);
		String key = jobID + "WeatherPipeMapreduce.jar";
		assertEquals("s3n://" + jobBucketName + "/" + key, awsInterface.UploadMPJarFile("WeatherPipeMapReduce.jar"));
	}
}