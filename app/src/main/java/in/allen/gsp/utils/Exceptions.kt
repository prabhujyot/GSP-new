package `in`.allen.gsp.utils

import java.io.IOException

class ApiException(message: String) : IOException(message)
class NoInternetExeption(message: String) : IOException(message)