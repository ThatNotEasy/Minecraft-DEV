/*     */ package net.minecraft.bundler;
/*     */ import java.io.BufferedReader;
/*     */ import java.io.InputStream;
/*     */ import java.io.InputStreamReader;
/*     */ import java.io.OutputStream;
/*     */ import java.lang.invoke.MethodHandle;
/*     */ import java.lang.invoke.MethodHandles;
/*     */ import java.lang.invoke.MethodType;
/*     */ import java.net.URL;
/*     */ import java.net.URLClassLoader;
/*     */ import java.nio.charset.StandardCharsets;
/*     */ import java.nio.file.CopyOption;
/*     */ import java.nio.file.Files;
/*     */ import java.nio.file.Path;
/*     */ import java.nio.file.Paths;
/*     */ import java.nio.file.StandardCopyOption;
/*     */ import java.nio.file.attribute.FileAttribute;
/*     */ import java.security.DigestOutputStream;
/*     */ import java.security.MessageDigest;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ 
/*     */ public class Main {
/*     */   public static void main(String[] argv) {
/*  25 */     (new Main()).run(argv);
/*     */   }
/*     */   
/*     */   private void run(String[] argv) {
/*     */     try {
/*  30 */       String defaultMainClassName = readResource("main-class", BufferedReader::readLine);
/*  31 */       String mainClassName = System.getProperty("bundlerMainClass", defaultMainClassName);
/*  32 */       String repoDir = System.getProperty("bundlerRepoDir", "");
/*     */       
/*  34 */       Path outputDir = Paths.get(repoDir, new String[0]);
/*  35 */       Files.createDirectories(outputDir, (FileAttribute<?>[])new FileAttribute[0]);
/*     */       
/*  37 */       List<URL> extractedUrls = new ArrayList<>();
/*  38 */       readAndExtractDir("versions", outputDir, extractedUrls);
/*  39 */       readAndExtractDir("libraries", outputDir, extractedUrls);
/*     */       
/*  41 */       if (mainClassName == null || mainClassName.isEmpty()) {
/*  42 */         System.out.println("Empty main class specified, exiting");
/*  43 */         System.exit(0);
/*     */       } 
/*     */       
/*  46 */       ClassLoader maybePlatformClassLoader = getClass().getClassLoader().getParent();
/*  47 */       URLClassLoader classLoader = new URLClassLoader(extractedUrls.<URL>toArray(new URL[0]), maybePlatformClassLoader);
/*  48 */       System.out.println("Starting " + mainClassName);
/*     */       
/*  50 */       Thread runThread = new Thread(() -> {
/*     */             try {
/*     */               Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
/*     */               MethodHandle mainHandle = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class)).asFixedArity();
/*     */               mainHandle.invoke(argv);
/*  55 */             } catch (Throwable t) {
/*     */               Thrower.INSTANCE.sneakyThrow(t);
/*     */             } 
/*     */           }"ServerMain");
/*     */ 
/*     */       
/*  61 */       runThread.setContextClassLoader(classLoader);
/*  62 */       runThread.start();
/*  63 */     } catch (Exception e) {
/*     */       
/*  65 */       e.printStackTrace(System.out);
/*  66 */       System.out.println("Failed to extract server libraries, exiting");
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private <T> T readResource(String resource, ResourceParser<T> parser) throws Exception
/*     */   {
/*  76 */     String fullPath = "/META-INF/" + resource;
/*     */     
/*  78 */     InputStream is = getClass().getResourceAsStream(fullPath); 
/*  79 */     try { if (is == null) {
/*  80 */         throw new IllegalStateException("Resource " + fullPath + " not found");
/*     */       }
/*  82 */       T t = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
/*  83 */       if (is != null) is.close();  return t; } catch (Throwable throwable) { if (is != null)
/*     */         try { is.close(); }
/*     */         catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }
/*     */           throw throwable; }
/*  87 */      } private void readAndExtractDir(String subdir, Path outputDir, List<URL> extractedUrls) throws Exception { List<FileEntry> entries = readResource(subdir + ".list", reader -> reader.lines().map(FileEntry::parseLine).toList());
/*     */     
/*  89 */     Path subdirPath = outputDir.resolve(subdir);
/*  90 */     for (FileEntry entry : entries) {
/*  91 */       Path outputFile = subdirPath.resolve(entry.path);
/*  92 */       checkAndExtractJar(subdir, entry, outputFile);
/*  93 */       extractedUrls.add(outputFile.toUri().toURL());
/*     */     }  }
/*     */ 
/*     */   
/*     */   private void checkAndExtractJar(String subdir, FileEntry entry, Path outputFile) throws Exception {
/*  98 */     if (!Files.exists(outputFile, new java.nio.file.LinkOption[0]) || !checkIntegrity(outputFile, entry.hash())) {
/*  99 */       System.out.printf("Unpacking %s (%s:%s) to %s%n", new Object[] { entry.path, subdir, entry.id, outputFile });
/* 100 */       extractJar(subdir, entry.path, outputFile);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void extractJar(String subdir, String jarPath, Path outputFile) throws IOException {
/* 105 */     Files.createDirectories(outputFile.getParent(), (FileAttribute<?>[])new FileAttribute[0]);
/* 106 */     InputStream input = getClass().getResourceAsStream("/META-INF/" + subdir + "/" + jarPath); 
/* 107 */     try { if (input == null) {
/* 108 */         throw new IllegalStateException("Declared library " + jarPath + " not found");
/*     */       }
/* 110 */       Files.copy(input, outputFile, new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
/* 111 */       if (input != null) input.close();  } catch (Throwable throwable) { if (input != null)
/*     */         try { input.close(); }
/*     */         catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }
/*     */           throw throwable; }
/* 115 */      } private static boolean checkIntegrity(Path file, String expectedHash) throws Exception { MessageDigest digest = MessageDigest.getInstance("SHA-256");
/*     */     
/* 117 */     InputStream output = Files.newInputStream(file, new java.nio.file.OpenOption[0]); 
/* 118 */     try { output.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
/* 119 */       String actualHash = byteToHex(digest.digest());
/* 120 */       if (actualHash.equalsIgnoreCase(expectedHash))
/* 121 */       { boolean bool = true;
/*     */ 
/*     */         
/* 124 */         if (output != null) output.close();  return bool; }  System.out.printf("Expected file %s to have hash %s, but got %s%n", new Object[] { file, expectedHash, actualHash }); if (output != null) output.close();  } catch (Throwable throwable) { if (output != null)
/* 125 */         try { output.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  return false; }
/*     */ 
/*     */   
/*     */   private static String byteToHex(byte[] bytes) {
/* 129 */     StringBuilder result = new StringBuilder(bytes.length * 2);
/* 130 */     for (byte b : bytes) {
/* 131 */       result.append(Character.forDigit(b >> 4 & 0xF, 16));
/* 132 */       result.append(Character.forDigit(b >> 0 & 0xF, 16));
/*     */     } 
/* 134 */     return result.toString();
/*     */   } @FunctionalInterface
/*     */   private static interface ResourceParser<T> {
/* 137 */     T parse(BufferedReader param1BufferedReader) throws Exception; } private static final class FileEntry extends Record { private final String hash; private final String id; private final String path; private FileEntry(String hash, String id, String path) { this.hash = hash; this.id = id; this.path = path; } public final String toString() { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: <illegal opcode> toString : (Lnet/minecraft/bundler/Main$FileEntry;)Ljava/lang/String;
/*     */       //   6: areturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #137	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/* 137 */       //   0	7	0	this	Lnet/minecraft/bundler/Main$FileEntry; } public String hash() { return this.hash; } public final int hashCode() { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: <illegal opcode> hashCode : (Lnet/minecraft/bundler/Main$FileEntry;)I
/*     */       //   6: ireturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #137	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/*     */       //   0	7	0	this	Lnet/minecraft/bundler/Main$FileEntry; } public final boolean equals(Object o) { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: aload_1
/*     */       //   2: <illegal opcode> equals : (Lnet/minecraft/bundler/Main$FileEntry;Ljava/lang/Object;)Z
/*     */       //   7: ireturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #137	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/*     */       //   0	8	0	this	Lnet/minecraft/bundler/Main$FileEntry;
/* 137 */       //   0	8	1	o	Ljava/lang/Object; } public String id() { return this.id; } public String path() { return this.path; }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     public static FileEntry parseLine(String line) {
/* 143 */       String[] fields = line.split("\t");
/* 144 */       if (fields.length != 3) {
/* 145 */         throw new IllegalStateException("Malformed library entry: " + line);
/*     */       }
/* 147 */       return new FileEntry(fields[0], fields[1], fields[2]);
/*     */     } }
/*     */ 
/*     */   
/*     */   private static class Thrower<T extends Throwable> {
/* 152 */     private static final Thrower<RuntimeException> INSTANCE = new Thrower();
/*     */ 
/*     */     
/*     */     public void sneakyThrow(Throwable exception) throws T {
/* 156 */       throw (T)exception;
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\apido\Desktop\MineCraft-Dev\server.jar!\net\minecraft\bundler\Main.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */