class GlobalCMVN(Module):
  __parameters__ = []
  __buffers__ = ["mean", "istd", ]
  mean : Tensor
  istd : Tensor
  training : bool
  norm_var : bool
  def forward(self: __torch__.wenet.transformer.cmvn.GlobalCMVN,
    x: Tensor) -> Tensor:
    x0 = torch.sub(x, self.mean, alpha=1)
    if self.norm_var:
      x1 = torch.mul(x0, self.istd)
    else:
      x1 = x0
    return x1
