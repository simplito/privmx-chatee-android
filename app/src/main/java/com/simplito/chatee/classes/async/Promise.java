//
// PrivMX Chatee Android.
// Copyright © 2024 Simplito sp. z o.o.
//
// This file is part of demonstration software for the PrivMX Platform (https://privmx.dev).
// This software is Licensed under the MIT License.
//
// See the License for the specific language governing permissions and
// limitations under the License.
//


package com.simplito.chatee.classes.async;

import java.util.concurrent.Executor;

public interface Promise<T> {

    Promise<T> then(Callback<T> callback);

    Promise<T> then(Executor executor, Callback<T> callback);

    Promise<T> fail(Callback<Exception> callback);

    Promise<T> fail(Executor executor, Callback<Exception> callback);
}