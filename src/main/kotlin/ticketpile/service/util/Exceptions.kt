package ticketpile.service.util

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Created by jonlatane on 8/30/16.
 */


@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ResourceNotFoundException(s:String) : RuntimeException(s) {

}

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class BadRequestException(s:String) : RuntimeException(s) {

}

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
class AuthenticationException(s:String) : RuntimeException(s) {
    
}