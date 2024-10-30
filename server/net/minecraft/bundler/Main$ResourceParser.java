package net.minecraft.bundler;

import java.io.BufferedReader;

@FunctionalInterface
interface ResourceParser<T> {
  T parse(BufferedReader paramBufferedReader) throws Exception;
}


/* Location:              C:\Users\apido\Desktop\MineCraft-Dev\server.jar!\net\minecraft\bundler\Main$ResourceParser.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */