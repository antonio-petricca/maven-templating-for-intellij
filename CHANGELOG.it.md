# maven-templating-for-intellij Changelog

## [2.0.0]

- Le cartelle dei template sono ora dichiarate durante l'import di Maven, leggendole dal pom, quindi non si perdono più al reimport del progetto.
- Nuova opzione per escludere i sorgenti generati, attiva di default, per evitare classi duplicate: esclude solo le cartelle di output del templating-maven-plugin, solo nei moduli che lo dichiarano, e ha effetto dopo un reimport di Maven.
- Navigazione tra un template e il suo sorgente generato, con icone nel margine distinte, un banner in entrambi i file e azioni di menu; prima della prima build, il banner sul template invita a compilare il progetto e Vai al sorgente generato è disabilitata.
- Nuovo sottomenu **Maven Templating** nei menu del tasto destro dell'editor, dei tab dell'editor e della vista Project, disponibile anche nel menu Tools, con una voce Impostazioni, e con Vai al sorgente generato / Vai al template nel menu Navigate e nel sottomenu Go To dell'editor.
- Nuova scorciatoia per Vai al sorgente generato / Vai al template: Ctrl+Alt+Shift+Space su Windows e Linux, Cmd+Alt+Shift+G su macOS.
- Completamento, navigazione e ispezione delle proprietà Maven ${...} usate nei template, con una quick fix che aggiunge al pom una proprietà mancante e lo apre con il cursore nel valore vuoto.
- Impostazioni riviste, con validazione e localizzazione in italiano; il nome della cartella dei template vale ora solo per i moduli che non dichiarano il templating-maven-plugin (per gli altri decide il pom).
- Notifica con l'esito al termine di una scansione richiesta dall'utente.
- Scansione del progetto più robusta e veloce, anche sui progetti di grandi dimensioni.
- Guida utente in inglese e italiano, disponibile dai menu Help e Tools | Maven Templating e dalla pagina delle impostazioni, in una finestra del plugin.
- Un tab Novità dopo ogni installazione e aggiornamento, con lo stile del tema dell'IDE e le novità della versione installata (disattivabile dalle impostazioni, per tutti i progetti).
- Compatibile con IntelliJ IDEA dalla 2026.2 (build 262) alla 2026.3 (build 263.*); le versioni precedenti non sono più supportate.

## [1.7.2]

- Dichiarata la compatibilità dal 2024 al 2026.2.
- Corretta la compilazione del progetto.

## [1.7.1]

- Dichiarata la compatibilità dal 2024 al 2026.1.
- Ridotto a 1 ms il ritardo della scansione (per i progetti di grandi dimensioni).

## [1.7.0]

- Dichiarata la compatibilità dal 2024 al 2025.3.
- Aggiornamento parziale al framework dei plugin 2.x.

## [1.6.0]

- Corretto: "Plugin depends on the Kotlin plugin (org.jetbrains.kotlin) but does not declare a compatibility mode in the <org.jetbrains.kotlin.supportsKotlinPluginMode> extension. This feature is available for IntelliJ IDEA 2024.2.1 or later. See https://kotlin.github.io/analysis-api/migrating-from-k1.html#declaring-compatibility-with-the-k2-kotlin-mode".
- Gestita con eleganza l'assenza del plugin Maven, con un avviso nel log.
- Rimosse le dipendenze non necessarie.

## [1.5.2]

- Dichiarata la compatibilità con la 2025.2.

## [1.5.1]

- Dichiarata la compatibilità con la 2025.1.

## [1.5.0]

- Aggiunto un indicatore di avanzamento della scansione del progetto.
- Consentita la parola "intellij" nell'id del plugin.
- Corretto: "Deprecated class ProjectFileIndex.SERVICE is referenced in ApiHelpers.Companion.getModelForFile$lambda$0(...)".
- Corretto: "Deprecated class ServiceManager is referenced in SettingsStorage.Companion.getInstance(Project)".
- Corretto: "Deprecated constructor URL.<init>(String) is invoked in VFSListener.doMoveSourceFolderBefore(...)" e "Deprecated constructor URL.<init>(String) is invoked in VFSListener.doMoveSourceFolderAfter(...)".
- Corretto: "Deprecated constructor java.net.URL.<init>(java.lang.String spec) is invoked in com.github.intellij.plugins.mt4ij.listeners.VFSListener.doMoveSourceFolderAfter(Project, String, VirtualFile, boolean) : void</init>".
- Corretto: "Deprecated method MavenProjectsManager.addManagerListener(...) is invoked in ProjectStartupActivity.registerMavenListener(...)".
- Corretto: "Deprecated method ServiceManager.getService(Project, Class) is invoked in SettingsStorage.Companion.getInstance(Project)".
- Corretto: "Internal method Module.getModuleTypeName() is invoked in ApiHelpers.Companion.getModelForFile(...). This method is marked with @ApiStatus.Internal annotation or @IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.".
- Piccola pulizia del codice.
- Form delle impostazioni: corretto il contesto del thread di invocazione.
- Form delle impostazioni: corretta la mancata visualizzazione.
- Form delle impostazioni: aggiornata l'etichetta Build Tools.
- Aggiornato il file README.md.

## [1.4.5]

- Dichiarata la compatibilità con la 2024.3.

## [1.4.4]

- Dichiarata la compatibilità con la 2024.2.

## [1.4.3]

- Corretta la compatibilità con la 2024.1.

## [1.4.2]

- Migrato com.github.intellij.plugins.mt4ij.activities.ProjectStartupActivity a ProjectActivity.

## [1.4.1]

- Dichiarata la compatibilità con la 2024.1.

## [1.4.0]

- Dichiarata la compatibilità con #IC-233.*.

## [1.3.1]

- Correzione per "com.intellij.diagnostic.PluginException: No display name is specified for configurable com.github.intellij.plugins.mt4ij.config.SettingsConfigurable in xml file;".

## [1.3.0]

- Dichiarata la compatibilità con #IC-232.*.

## [1.2.1]

- Sistemato il CHANGELOG che non mostrava le modifiche della 1.2.0.
- Corretto: "Must not use executable property on Test together with javaLauncher property".

## [1.2.0]

- Aggiornamento dei plugin di Gradle.
- Corretto: "java.lang.Throwable: Read access is allowed from inside read-action (or EDT) only (see com.intellij.openapi.application.Application.runReadAction())".

## [1.1.1]

- Dichiarata la compatibilità con #IC-231.*.

## [1.1.0]

- Dichiarata la compatibilità con #IC-223.*.

## [1.0.11]

- Dichiarata la compatibilità con #IC-222.*.

## [1.0.10]

- Dichiarata la compatibilità con #IC-221.5080.210 (2022.1).

## [1.0.9]

- Dichiarata la compatibilità con #IC-221.5080.210 (2022.1).

## [1.0.8]

- Implementato il listener per l'evento di import del progetto Maven.
- Migliorata la dipendenza dal plugin Java IDE e da quello Maven.
- Migliorate le istruzioni d'uso.
- Migliorato/corretto il listener del VFS.
- Correzione di un refuso.

## [1.0.7]

- Correzione di un refuso.

## [1.0.6]

- Implementato il listener per l'evento di import del progetto Maven.
- Migliorata la dipendenza dal plugin Java IDE e da quello Maven.
- Migliorate le istruzioni d'uso.
- Migliorato/corretto il listener del VFS.

## [1.0.5]

- Aggiunta la scansione del progetto come voce del menu popup della vista Project.

## [1.0.4]

- Abilitata la build QODANA anche per il branch **develop**.
- Corretti i suggerimenti di QODANA.
- Gestito (di nuovo) il timeout di recupero del progetto attivo.

## [1.0.3]

- Corretto il CHANGELOG per la mancanza di **Unreleased**, che causava il fallimento della build su **GitHub**.

## [1.0.2]

- Aggiunta la scansione del progetto all'applicazione delle impostazioni.
- Aggiunta la scansione del progetto come voce del menu **Tools**.
- Reso ricercabile il pannello delle impostazioni.
- Ridotto il timeout di recupero del progetto attivo.

## [1.0.1]

- Aggiunta la scansione post-caricamento del progetto per le cartelle source/test mancanti.
- Corretta l'eccezione di timeout nel recupero del progetto attivo.
- Mancava : nell'etichetta del pannello di configurazione.

## [1.0.0]

### Modificato

- Corretti gli aggiornamenti non sicuri delle cartelle sorgente del progetto.
- Spostato il file delle impostazioni **mt4ij.xml** nella cartella **.idea** del progetto (grazie a **Vojtěch Krása** e **Petr Makhnev**).

## [0.0.3]

### Modificato

- Aggiunta la dichiarazione della dipendenza **org.jetbrains.idea.maven**.
- Personalizzata l'icona del plugin.

## [0.0.2]

### Modificato

- Corretto il rilevamento del nome della cartella da **contains** a **endsWith**.

## [0.0.1]

### Aggiunto

- Revisione iniziale del plugin (contiene qualche piccolo bug architetturale).
