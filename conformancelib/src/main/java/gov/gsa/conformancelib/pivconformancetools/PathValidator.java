package gov.gsa.conformancelib.pivconformancetools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import gov.gsa.conformancelib.utilities.Utils;

public class PathValidator {

	public static CertPath buildCertPath(X509Certificate eeCert, X509Certificate trustAnchorCert, KeyStore ks,
			String certPolicyOid) {
		try {
			CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
			X509CertSelector eeCertSelector = new X509CertSelector();
			eeCertSelector.setSubject(eeCert.getSubjectX500Principal().getEncoded());

			List<X509Certificate> certList = new ArrayList<>();
			certList.add(eeCert);
			Enumeration<String> enumeration = ks.aliases();
			Set<TrustAnchor> trustAnchors = new HashSet<>();
			trustAnchors.add(new TrustAnchor((X509Certificate) trustAnchorCert, null));
			while (enumeration.hasMoreElements()) {
				X509Certificate certificate = (X509Certificate) ks.getCertificate(enumeration.nextElement());
				System.out.println(certificate.getSerialNumber() + ": " + certificate.getSubjectX500Principal()
				+ " issued by " + certificate.getIssuerX500Principal());
				if (!certificate.equals(trustAnchorCert)) {
					certList.add(certificate);
				}
			}

			CertStore certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList));
			PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, eeCertSelector);
			params.addCertStore(certStore);
			params.setRevocationEnabled(false);
			HashSet<String> policies = new HashSet<String>();
			policies.add(certPolicyOid);
			params.setInitialPolicies(policies);
			params.setExplicitPolicyRequired(true);
			params.setMaxPathLength(6);
			params.setDate(new Date());

			PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) builder.build(params);

			return result.getCertPath();

		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
			System.out.println("Can't build certificate chain: " + ex.getMessage());
		} catch (CertPathBuilderException ex) {
			System.out.println("Policy error: " + ex.getMessage());
		}
		return null;
	}

	public static void main(String args[]) throws Exception {
		// create certificates and CRLs
		java.security.Security.addProvider(new BouncyCastleProvider());
		String cwd = Utils.pathFixup(PathValidator.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		System.out.println("Current directory is " + cwd);

		// Start args parsing
		String keystorePath = args[0];
		String keystorePass = args[1];
		String trustAnchorAlias = args[2];
		FileInputStream eeIs = new FileInputStream(args[3]);
		String certPolicyOid = args[4];
		// End args parsing

		CertificateFactory eeCf = CertificateFactory.getInstance("X.509");
		X509Certificate eeCert = (X509Certificate) eeCf.generateCertificate(eeIs);
		eeIs.close();
		KeyStore keyStore = Utils.loadKeyStore(keystorePath, keystorePass);
		X509Certificate trustAnchorCert = (X509Certificate) keyStore.getCertificate(trustAnchorAlias);

		CertPath certPath = buildCertPath(eeCert, trustAnchorCert, keyStore, certPolicyOid);

		@SuppressWarnings("unchecked")
		List<X509Certificate> certs = (List<X509Certificate>) certPath.getCertificates();

		Iterator<X509Certificate> it = certs.iterator();
		while (it.hasNext()) {
			System.out.println(((X509Certificate) it.next()).getSubjectX500Principal());
		}
	}
}