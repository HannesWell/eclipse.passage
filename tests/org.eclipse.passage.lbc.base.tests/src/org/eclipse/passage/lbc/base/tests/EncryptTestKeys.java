package org.eclipse.passage.lbc.base.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.passage.lic.api.LicensedProduct;
import org.eclipse.passage.lic.api.LicensingException;
import org.eclipse.passage.lic.api.io.StreamCodec;
import org.eclipse.passage.lic.base.BaseLicensedProduct;
import org.eclipse.passage.lic.base.io.FloatingFileExtension;
import org.eclipse.passage.lic.bc.BcStreamCodec;
import org.eclipse.passage.lic.internal.emf.EObjectFromBytes;
import org.eclipse.passage.lic.keys.model.api.KeyPair;
import org.eclipse.passage.loc.internal.api.workspace.Keys;
import org.eclipse.passage.loc.internal.licenses.core.i18n.LicensesCoreMessages;
import org.eclipse.passage.loc.internal.products.core.i18n.IssuingMessages;

@SuppressWarnings("restriction")
public class EncryptTestKeys {

	static {
		EcorePlugin.ExtensionProcessor.process(EncryptTestKeys.class.getClassLoader());
	}

	public static void main(String[] args) throws LicensingException {
		Path base = Path.of(".", "resource", "lics").toAbsolutePath(); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		LicensedProduct product = new BaseLicensedProduct("anti-human-magic.product", "0.2.1"); //$NON-NLS-1$ //$NON-NLS-2$
		PersistedEncoded.write(base, product, "d05f5266-9340-48a5-8107-a9727a76d8ba"); //$NON-NLS-1$
	}

	private static final class PersistedEncoded {

		static Path write(Path base, LicensedProduct product, String licenseId) throws LicensingException {
			String password = product.identifier() + "###" + product.version(); //$NON-NLS-1$
			Path licenseDir = base.resolve(product.identifier()).resolve(product.version()).resolve(licenseId);
			Path decrypted = licenseDir.resolve(licenseId + new FloatingFileExtension.FloatingLicenseDecrypted().get());
			Path encrypted = licenseDir.resolve(licenseId + new FloatingFileExtension.FloatingLicenseEncrypted().get());

			try (InputStream input = Files.newInputStream(decrypted);
					OutputStream output = Files.newOutputStream(encrypted);
					InputStream key = ProductKeys.privateKeyStream(base, product)) {
				StreamCodec codec = new BcStreamCodec(() -> product);
				codec.encode(input, output, key, product.identifier(), password);
				return encrypted;
			} catch (IOException e) {
				throw new LicensingException(
						String.format(LicensesCoreMessages.LicenseOperatorServiceImpl_floating_save_encoded_file_error,
								encrypted.toAbsolutePath()),
						e);
			}
		}
	}

	private static class ProductKeys {

		public static InputStream privateKeyStream(Path base, LicensedProduct product) throws LicensingException {
			return new ByteArrayInputStream(getBytes(base, product, KeyPair::getScr));
		}

		private static byte[] getBytes(Path base, LicensedProduct product, Function<KeyPair, String> get)
				throws LicensingException {
			Path keysFile = base.resolve(product.identifier()).resolve(product.version())
					.resolve(product.identifier() + "_" + product.version() + "." + Keys.xmi.id()); //$NON-NLS-1$ //$NON-NLS-2$

			if (!Files
					.isRegularFile(keysFile)/* keys.existing(product.identifier(), product.version()).isPresent() */) {
				throw new LicensingException(
						String.format(IssuingMessages.ProductKeys_keys_no_storage_for_product, product));
			}
			try (var s = Files.newInputStream(keysFile)) {
				byte[] content = s.readAllBytes();
				KeyPair pair = new EObjectFromBytes<>(content, KeyPair.class, XMIResourceImpl::new).get();
				return get.apply(pair).getBytes();
			} catch (Exception e) {
				throw new LicensingException(
						String.format(IssuingMessages.ProductKeys_keys_reading_failed, product, keysFile));
			}
		}
	}
}
