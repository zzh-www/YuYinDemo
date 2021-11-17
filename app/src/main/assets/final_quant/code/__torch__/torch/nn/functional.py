def log_softmax(input: Tensor,
    dim: Optional[int]=None,
    _stacklevel: int=3,
    dtype: Optional[int]=None) -> Tensor:
  _0 = __torch__.torch.nn.functional._get_softmax_dim
  if torch.__is__(dim, None):
    dim1 = _0("log_softmax", torch.dim(input), _stacklevel, )
    dim0 = dim1
  else:
    dim0 = unchecked_cast(int, dim)
  if torch.__is__(dtype, None):
    ret0 = torch.log_softmax(input, dim0, None)
    ret = ret0
  else:
    dtype0 = unchecked_cast(int, dtype)
    ret1 = torch.log_softmax(input, dim0, dtype0)
    ret = ret1
  return ret
def relu(input: Tensor,
    inplace: bool=False) -> Tensor:
  if inplace:
    result = torch.relu_(input)
  else:
    result = torch.relu(input)
  return result
def dropout(input: Tensor,
    p: float=0.5,
    training: bool=True,
    inplace: bool=False) -> Tensor:
  if torch.lt(p, 0.):
    _1 = True
  else:
    _1 = torch.gt(p, 1.)
  if _1:
    ops.prim.RaiseException("Exception")
  else:
    pass
  if inplace:
    _2 = torch.dropout_(input, p, training)
  else:
    _2 = torch.dropout(input, p, training)
  return _2
def layer_norm(input: Tensor,
    normalized_shape: List[int],
    weight: Optional[Tensor]=None,
    bias: Optional[Tensor]=None,
    eps: float=1.0000000000000001e-05) -> Tensor:
  _3 = torch.layer_norm(input, normalized_shape, weight, bias, eps, True)
  return _3
def _pad(input: Tensor,
    pad: List[int],
    mode: str="constant",
    value: float=0.) -> Tensor:
  _4 = __torch__.torch.nn.functional._pad_circular
  _5 = uninitialized(Tensor)
  _6 = torch.eq(torch.remainder(torch.len(pad), 2), 0)
  if _6:
    pass
  else:
    ops.prim.RaiseException("Exception")
  _7 = torch.le(torch.floordiv(torch.len(pad), 2), torch.dim(input))
  if _7:
    pass
  else:
    ops.prim.RaiseException("Exception")
  if torch.eq(mode, "constant"):
    _9 = torch.constant_pad_nd(input, pad, value)
    _8 = _9
  else:
    if torch.eq(value, 0):
      pass
    else:
      ops.prim.RaiseException("Exception")
    if torch.eq(torch.dim(input), 3):
      if torch.eq(torch.len(pad), 2):
        pass
      else:
        ops.prim.RaiseException("Exception")
      if torch.eq(mode, "reflect"):
        _12 = torch.reflection_pad1d(input, pad)
        _11 = _12
      else:
        if torch.eq(mode, "replicate"):
          _14 = torch.replication_pad1d(input, pad)
          _13 = _14
        else:
          if torch.eq(mode, "circular"):
            _15 = _4(input, pad, )
          else:
            ops.prim.RaiseException("Exception")
            _15 = _5
          _13 = _15
        _11 = _13
      _10 = _11
    else:
      if torch.eq(torch.dim(input), 4):
        if torch.eq(torch.len(pad), 4):
          pass
        else:
          ops.prim.RaiseException("Exception")
        if torch.eq(mode, "reflect"):
          _18 = torch.reflection_pad2d(input, pad)
          _17 = _18
        else:
          if torch.eq(mode, "replicate"):
            _20 = torch.replication_pad2d(input, pad)
            _19 = _20
          else:
            if torch.eq(mode, "circular"):
              _21 = _4(input, pad, )
            else:
              ops.prim.RaiseException("Exception")
              _21 = _5
            _19 = _21
          _17 = _19
        _16 = _17
      else:
        if torch.eq(torch.dim(input), 5):
          if torch.eq(torch.len(pad), 6):
            pass
          else:
            ops.prim.RaiseException("Exception")
          if torch.eq(mode, "reflect"):
            ops.prim.RaiseException("Exception")
            _23 = _5
          else:
            if torch.eq(mode, "replicate"):
              _25 = torch.replication_pad3d(input, pad)
              _24 = _25
            else:
              _26 = torch.eq(mode, "circular")
              if _26:
                _27 = _4(input, pad, )
              else:
                ops.prim.RaiseException("Exception")
                _27 = _5
              _24 = _27
            _23 = _24
          _22 = _23
        else:
          ops.prim.RaiseException("Exception")
          _22 = _5
        _16 = _22
      _10 = _16
    _8 = _10
  return _8
def glu(input: Tensor,
    dim: int=-1) -> Tensor:
  if torch.eq(torch.dim(input), 0):
    ops.prim.RaiseException("Exception")
  else:
    pass
  return torch.glu(input, dim)
def embedding(input: Tensor,
    weight: Tensor,
    padding_idx: Optional[int]=None,
    max_norm: Optional[float]=None,
    norm_type: float=2.,
    scale_grad_by_freq: bool=False,
    sparse: bool=False) -> Tensor:
  if torch.__isnot__(padding_idx, None):
    padding_idx1 = unchecked_cast(int, padding_idx)
    if torch.gt(padding_idx1, 0):
      _28 = torch.lt(padding_idx1, torch.size(weight, 0))
      if _28:
        pass
      else:
        ops.prim.RaiseException("Exception")
      padding_idx2 = padding_idx1
    else:
      if torch.lt(padding_idx1, 0):
        _29 = torch.neg(torch.size(weight, 0))
        if torch.ge(padding_idx1, _29):
          pass
        else:
          ops.prim.RaiseException("Exception")
        padding_idx4 = torch.add(torch.size(weight, 0), padding_idx1)
        padding_idx3 = padding_idx4
      else:
        padding_idx3 = padding_idx1
      padding_idx2 = padding_idx3
    padding_idx0 = padding_idx2
  else:
    padding_idx0 = -1
  if torch.__isnot__(max_norm, None):
    input1 = torch.contiguous(input, memory_format=0)
    input0 = input1
  else:
    input0 = input
  _30 = torch.embedding(weight, input0, padding_idx0, scale_grad_by_freq, sparse)
  return _30
def ctc_loss(log_probs: Tensor,
    targets: Tensor,
    input_lengths: Tensor,
    target_lengths: Tensor,
    blank: int=0,
    reduction: str="mean",
    zero_infinity: bool=False) -> Tensor:
  _31 = __torch__.torch.nn._reduction.get_enum
  _32 = torch.ctc_loss(log_probs, targets, input_lengths, target_lengths, blank, _31(reduction, ), zero_infinity)
  return _32
def kl_div(input: Tensor,
    target: Tensor,
    size_average: Optional[bool]=None,
    reduce: Optional[bool]=None,
    reduction: str="mean",
    log_target: bool=False) -> Tensor:
  _33 = __torch__.torch.nn._reduction.legacy_get_enum
  _34 = "reduction: \'mean\' divides the total loss by both the batch size and the support size.\'batchmean\' divides only by the batch size, and aligns with the KL div math definition.\'mean\' will be changed to behave the same as \'batchmean\' in the next major release."
  _35 = __torch__.torch.nn._reduction.get_enum
  if torch.__isnot__(size_average, None):
    _36, size_average0 = True, unchecked_cast(bool, size_average)
  else:
    _36, size_average0 = torch.__isnot__(reduce, None), size_average
  if _36:
    reduction_enum = _33(size_average0, reduce, True, )
  else:
    if torch.eq(reduction, "mean"):
      torch.warn(_34, 2)
    else:
      pass
    if torch.eq(reduction, "batchmean"):
      reduction_enum0 = _35("sum", )
    else:
      reduction_enum0 = _35(reduction, )
    reduction_enum = reduction_enum0
  reduced = torch.kl_div(input, target, reduction_enum, log_target=log_target)
  if torch.eq(reduction, "batchmean"):
    _37 = torch.ne(torch.dim(input), 0)
  else:
    _37 = False
  if _37:
    reduced1 = torch.div(reduced, (torch.size(input))[0])
    reduced0 = reduced1
  else:
    reduced0 = reduced
  return reduced0
def _get_softmax_dim(name: str,
    ndim: int,
    stacklevel: int) -> int:
  _38 = "Implicit dimension choice for {} has been deprecated. Change the call to include dim=X as an argument."
  torch.warn(torch.format(_38, name), stacklevel)
  if torch.eq(ndim, 0):
    _39 = True
  else:
    _39 = torch.eq(ndim, 1)
  if _39:
    _40 = True
  else:
    _40 = torch.eq(ndim, 3)
  if _40:
    ret = 0
  else:
    ret = 1
  return ret
def _pad_circular(input: Tensor,
    padding: List[int]) -> Tensor:
  _41 = torch.slice(input, 0, 0, 9223372036854775807, 1)
  _42 = torch.slice(_41, 1, 0, 9223372036854775807, 1)
  _43 = torch.slice(_42, 2, 0, padding[-1], 1)
  input2 = torch.cat([input, _43], 2)
  _44 = torch.slice(input2, 0, 0, 9223372036854775807, 1)
  _45 = torch.slice(_44, 1, 0, 9223372036854775807, 1)
  _46 = torch.neg(torch.add(padding[-1], padding[-2]))
  _47 = torch.slice(_45, 2, _46, torch.neg(padding[-1]), 1)
  input3 = torch.cat([_47, input2], 2)
  if torch.gt(torch.len(padding), 2):
    _48 = torch.slice(input3, 0, 0, 9223372036854775807, 1)
    _49 = torch.slice(_48, 1, 0, 9223372036854775807, 1)
    _50 = torch.slice(_49, 2, 0, 9223372036854775807, 1)
    _51 = torch.slice(_50, 3, 0, padding[-3], 1)
    input5 = torch.cat([input3, _51], 3)
    _52 = torch.slice(input5, 0, 0, 9223372036854775807, 1)
    _53 = torch.slice(_52, 1, 0, 9223372036854775807, 1)
    _54 = torch.slice(_53, 2, 0, 9223372036854775807, 1)
    _55 = torch.neg(torch.add(padding[-3], padding[-4]))
    _56 = torch.slice(_54, 3, _55, torch.neg(padding[-3]), 1)
    input4 = torch.cat([_56, input5], 3)
  else:
    input4 = input3
  if torch.gt(torch.len(padding), 4):
    _57 = torch.slice(input4, 0, 0, 9223372036854775807, 1)
    _58 = torch.slice(_57, 1, 0, 9223372036854775807, 1)
    _59 = torch.slice(_58, 2, 0, 9223372036854775807, 1)
    _60 = torch.slice(_59, 3, 0, 9223372036854775807, 1)
    _61 = torch.slice(_60, 4, 0, padding[-5], 1)
    input7 = torch.cat([input4, _61], 4)
    _62 = torch.slice(input7, 0, 0, 9223372036854775807, 1)
    _63 = torch.slice(_62, 1, 0, 9223372036854775807, 1)
    _64 = torch.slice(_63, 2, 0, 9223372036854775807, 1)
    _65 = torch.slice(_64, 3, 0, 9223372036854775807, 1)
    _66 = torch.neg(torch.add(padding[-5], padding[-6]))
    _67 = torch.slice(_65, 4, _66, torch.neg(padding[-5]), 1)
    input6 = torch.cat([_67, input7], 4)
  else:
    input6 = input4
  return input6
